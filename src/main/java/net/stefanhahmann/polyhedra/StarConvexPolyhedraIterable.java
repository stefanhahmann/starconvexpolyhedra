package net.stefanhahmann.polyhedra;

import bdv.viewer.Source;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.KnownConstant;
import net.imglib2.roi.Regions;
import net.imglib2.roi.mask.integer.DefaultMask;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class StarConvexPolyhedraIterable< T > implements IterableInterval< T >, Localizable
{
	private final Source< T > source;

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	/**
	 * transform of current source to global coordinates
	 */
	private final AffineTransform3D sourceTransform = new AffineTransform3D();

	private IterableInterval< T > polyhedraVoxels;

	/**
	 * position of a single voxel in source coordinates
	 */
	private final double[] tempVoxel = new double[ 3 ];

	/*
	 * center of polyhedra in source coordinates
	 */
	double[] center = new double[ 3 ];

	/**
	 * bounding box min
	 */
	double[] minPoint = new double[ 3 ];

	/**
	 * bounding box max
	 */
	double[] maxPoint = new double[ 3 ];

	/**
	 * bounding box min
	 */
	// TODO why is this long?
	private final long[] min = new long[ 3 ];

	/**
	 * bounding box max
	 */
	private final long[] max = new long[ 3 ];

	public StarConvexPolyhedraIterable( final Source< T > source )
	{
		this.source = source;
	}

	@Override
	public Cursor< T > cursor()
	{
		return polyhedraVoxels.cursor();
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return polyhedraVoxels.localizingCursor();
	}

	@Override
	public long size()
	{
		return polyhedraVoxels.size();
	}

	@Override
	public T firstElement()
	{
		return polyhedraVoxels.firstElement();
	}

	@Override
	public Object iterationOrder()
	{
		return polyhedraVoxels.iterationOrder();
	}

	@SuppressWarnings( "all" )
	@Override
	public Iterator< T > iterator()
	{
		return polyhedraVoxels.iterator();
	}

	@Override
	public long min( int d )
	{
		return min[ d ];
	}

	@Override
	public long max( int d )
	{
		return max[ d ];
	}

	@Override
	public long getLongPosition( int d )
	{
		return Math.round( center[ d ] );
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	/**
	 * Resets this iterable to that it iterates over the specified polyhedra. The
	 * pixel iterated are taken from the resolution level 0,
	 *
	 * @param polyhedra
	 *            the polyhedra to iterate.
	 */
	public void reset( final StarConvexPolyhedra polyhedra, final int timepoint )
	{
		reset( polyhedra, timepoint, 0 );
	}

	/**
	 * Resets this iterable to that it iterates over the specified polyhedra, at the
	 * specified resolution level in the source. Generate an error of the
	 * specified resolution level is not present in the source.
	 *
	 * @param polyhedra
	 *            the polyhedra to iterate.
	 * @param resolutionLevel
	 *            the resolution level to use in the source.
	 */
	public void reset( final StarConvexPolyhedra polyhedra, final int timepoint, final int resolutionLevel )
	{
		// get source transform from source
		source.getSourceTransform( timepoint, resolutionLevel, sourceTransform );
		final RandomAccessibleInterval< T > img = source.getSource( timepoint, resolutionLevel );

		// transform spot position into source coordinates
		sourceTransform.inverse().apply( polyhedra.getCenter(), center );

		// transform polyhedra vertices into source coordinates
		List< double[] > vertices = new ArrayList<>( polyhedra.getPoints() );
		vertices.forEach( vertex -> sourceTransform.inverse().apply( vertex, vertex ) );

		// transform bounding box into source coordinates
		sourceTransform.inverse().apply( polyhedra.getBoundingBox3D().getMinPoint(), minPoint );
		sourceTransform.inverse().apply( polyhedra.getBoundingBox3D().getMaxPoint(), maxPoint );

		// transform lattice on unit sphere into source coordinates
		List< double[] > lattice = polyhedra.getLattice();
		lattice.forEach( vertex -> sourceTransform.inverse().apply( vertex, vertex ) );

		// get transformed bounding box with long coordinates
		for ( int d = 0; d < 3; d++ )
		{
			min[ d ] = Math.max( 0, ( long ) Math.floor( minPoint[ d ] ) );
			max[ d ] = Math.min( img.max( d ), ( long ) Math.ceil( maxPoint[ d ] ) );
		}
		logger.debug( "bounding volume of polyhedra defined by min ({},{},{}) and max ({},{},{}).", minPoint[ 0 ], minPoint[ 1 ],
				minPoint[ 2 ], maxPoint[ 0 ], maxPoint[ 1 ], maxPoint[ 2 ] );
		logger.debug( "bounding box of polyhedra in source coordinates defined by min ({},{},{}) and max ({},{},{}).", min[ 0 ], min[ 1 ],
				min[ 2 ], max[ 0 ], max[ 1 ], max[ 2 ] );
		logger.debug( "bounding volume, length in x direction: {}, length in y direction: {}, length in z direction: {}",
				max[ 0 ] - min[ 0 ], max[ 1 ] - min[ 1 ], max[ 2 ] - min[ 2 ] );
		logger.debug( "bounding volume size = {}", ( max[ 0 ] - min[ 0 ] ) * ( max[ 1 ] - min[ 1 ] ) * ( max[ 2 ] - min[ 2 ] ) );

		// if bounding box is empty, we set it to cover pixel at (0,0,0)
		// this will hopefully not cause problems, because it would not overlap
		// with polyhedra, so the polyhedra iterable could be empty.
		if ( Intervals.isEmpty( this ) )
			for ( int d = 0; d < 3; ++d )
				min[ d ] = max[ d ] = 0;

		// create a new polyhedra with the vertices transformed into source coordinates
		StarConvexPolyhedra transformedPolyhedra = new StarConvexPolyhedra( center, vertices, minPoint, maxPoint, lattice );

		// inflate polyhedra by .5 pixels on either side
		// TODO

		AtomicInteger count = new AtomicInteger( 0 );
		// create mask
		final Predicate< Localizable > contains = localizable -> {
			count.incrementAndGet();
			if ( count.get() % 100_000 == 0 )
				logger.debug( "contains count = {}", count.get() );
			localizable.localize( tempVoxel );
			// TODO this seems to get called more often than expected
			return transformedPolyhedra.contains( tempVoxel );
		};
		final DefaultMask mask = new DefaultMask( 3, BoundaryType.UNSPECIFIED, contains, KnownConstant.UNKNOWN );
		polyhedraVoxels = Regions.sampleWithMask( mask, Views.interval( img, this ) );
	}
}
