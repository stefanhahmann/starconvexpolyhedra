package net.stefanhahmann.stardist3d.jdll;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;

public class RandomAccessibleIntervalExample
{
	public static void main( String[] args )
	{
		// Create a RandomAccessibleInterval of FloatType with dimensions 10x10x5
		long[] dimensions = { 10, 10, 5 };
		RandomAccessibleInterval< FloatType > rai = new ArrayImgFactory<>( new FloatType() ).create( dimensions );

		// Initialize values in the RAI
		LoopBuilder.setImages( rai ).forEachPixel( floatType -> floatType.setReal( Math.random() ) );

		// Display in BigDataViewer
		BdvFunctions.show( rai, "FloatType RAI", BdvOptions.options() );

		RandomAccessibleInterval< FloatType > copy = ArrayImgs.floats( 10, 10, 5 );
		LoopBuilder.setImages( rai, copy ).forEachPixel( ( a, b ) -> b.set( a ) );

		// Create a scaled copy
		long[] scaledDimensions = { 20, 20, 10 };

		// Use interpolation and scaling
		AffineTransform3D transform = new AffineTransform3D();
		transform.scale( 2.0 ); // Scale by a factor of 2 in all dimensions

		// Transform the original RAI and copy into the scaled RAI
		IntervalView< FloatType > scaled = Views.interval(
				Views.raster( RealViews.transform( Views.interpolate( copy, new NearestNeighborInterpolatorFactory<>() ), transform ) ),
				new FinalInterval( scaledDimensions ) );

		// Display the scaled RAI in BigDataViewer
		BdvFunctions.show( scaled, "Scaled FloatType RAI", BdvOptions.options() );

	}
}
