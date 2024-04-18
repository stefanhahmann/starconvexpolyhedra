package net.stefanhahmann.stardist3d;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.stefanhahmann.polyhedron.StarConvexPolyhedron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

// TODO implementation of non-maximum suppression (NMS) is yet required,
// cf. https://github.com/stardist/stardist-imagej/blob/master/src/main/java/de/csbdresden/stardist/StarDist2DNMS.java
// otherwise there are too many non-maxima / irrelevant star-convex shapes in the output
public class StarDist
{

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	/**
	 * The predicted star convex shapes. Includes many irrelevant shapes.
	 * Non-maximum suppression (NMS) is yet required to reduce the number of shapes.
	 */
	private final List< StarConvexPolyhedron > starConvexPolyhedra = new ArrayList<>();

	public StarDist( RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities )
	{
		this( distances, probabilities, 0.4, 2 );
	}

	private StarDist( RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities,
			double threshold,
			int buffer )
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( distances );
		if ( dimensions.length != 5 )
			throw new IllegalArgumentException( "Input is expected to have 5 dimensions, but has: " + dimensions.length + "dimensions." );

		int numberOfRays = ( int ) dimensions[ 4 ];
		logger.debug( "numberOfRays = {}", numberOfRays );

		processTensors( distances.randomAccess(), probabilities.randomAccess(), threshold, buffer, dimensions, numberOfRays );
	}

	List< StarConvexPolyhedron > getStarConvexPolyhedra()
	{
		return starConvexPolyhedra;
	}

	private void processTensors(
			RandomAccess< FloatType > distances, final RandomAccess< FloatType > probabilities, double threshold, int buffer,
			long[] distancesTensors, int numberOfRays
	)
	{
		// origin is the center of the star convex shape
		logger.debug( "Computing star convex shapes." );
		logger.debug( "prediction computed within these bounds (excludes a buffer of {}): ", buffer );
		logger.debug( "x: ({} - {})", buffer, distancesTensors[ 0 ] - buffer );
		logger.debug( "y: ({} - {})", buffer, distancesTensors[ 1 ] - buffer );
		logger.debug( "z: ({} - {})", buffer, distancesTensors[ 2 ] - buffer );
		for ( int originX = buffer; originX < distancesTensors[ 0 ] - buffer; originX++ )
		{
			for ( int originY = buffer; originY < distancesTensors[ 1 ] - buffer; originY++ )
			{
				for ( int originZ = buffer; originZ < distancesTensors[ 2 ] - buffer; originZ++ )
				{
					final float score = probabilities.setPositionAndGet( originX, originY, originZ, 0, 0 ).getRealFloat();
					if ( score > threshold )
					{
						List< Double > distanceList = new ArrayList<>();
						for ( int i = 0; i < numberOfRays; i++ )
						{
							double distance = distances.setPositionAndGet( originX, originY, originZ, 0, i ).getRealDouble();
							distanceList.add( distance );
						}
						StarConvexPolyhedron polyhedra =
								new StarConvexPolyhedron( new double[] { originX, originY, originZ }, distanceList );
						starConvexPolyhedra.add( polyhedra );
					}
				}
			}
		}
		logger.debug( "Found {} candidate for star convex shapes above threshold of {} (including non-maximum shapes).",
				starConvexPolyhedra.size(), threshold );
		// TODO: add non-maximum suppression (NMS) after this
		// cf. https://github.com/stardist/stardist-imagej/blob/master/src/main/java/de/csbdresden/stardist/Candidates.java#L123
	}
}
