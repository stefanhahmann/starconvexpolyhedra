package net.stefanhahmann.polyhedron;

import bdv.util.BdvFunctions;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.ByteType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class StarConvexPolyhedronDemo
{
	public static void main( String[] args )
	{
		Img< ByteType > img = ArrayImgs.bytes( 100, 100, 100 );

		Source< ByteType > source = new RandomAccessibleIntervalSource<>( img, new ByteType(), "Star convex polyhedron demo" );

		StarConvexPolyhedronIterable< ByteType > polyhedronIterable = new StarConvexPolyhedronIterable<>( source );

		Random random = new Random();
		random.setSeed( 1 );
		List< Double > distances = random.doubles( 96, 5, 40 )
				.boxed()
				.collect( Collectors.toList() );
		StarConvexPolyhedron polyhedron = new StarConvexPolyhedron( new double[] { 50, 50, 50 }, distances );

		// TODO: can this be further optimized in terms of performance?
		polyhedronIterable.reset( polyhedron, 0 );
		polyhedronIterable.forEach( byteType -> {
			byteType.set( ( byte ) 100 );
		} );

		BdvFunctions.show( img, "Star convex polyhedron demo" );
	}
}
