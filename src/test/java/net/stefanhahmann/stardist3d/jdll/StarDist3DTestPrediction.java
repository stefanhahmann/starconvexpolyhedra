package net.stefanhahmann.stardist3d.jdll;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.apache.commons.compress.archivers.ArchiveException;

import bdv.BigDataViewer;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.model.Stardist3D;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

/**
 * <ul>
 *     <li>Model: <a href="https://bioimage.io/#/?tags=stardist&id=10.5281/zenodo.8421755">StarDist Plant Nuclei 3D ResNet</a> </li>
 *     <li>Data: <a href="https://www.ebi.ac.uk/biostudies/BioImages/studies/S-BIAD1026">Data from BioImage Archive</a></li>
 *     <li>Description: <a href="https://github.com/kreshuklab/go-nuclear">Description</a> </li>
 * </ul>
 */
public class StarDist3DTestPrediction
{
	public static < T extends RealType< T > & NativeType< T > > void main( String[] args )
			throws ModelSpecsException, IOException, InterruptedException, SpimDataException, MambaInstallException, URISyntaxException,
			ArchiveException, LoadEngineException, RunModelException, LoadModelException
	{
		String xmlFilePath = "1136.xml";
		SpimData spimData = new XmlIoSpimData().load( xmlFilePath );
		BigDataViewer bdv = BigDataViewer.open( spimData, "Plan Nuclei 3D", null, ViewerOptions.options() );

		int level = 3;
		int timePoint = 0;
		int setupId = 0;

		final SourceAndConverter< T > sourceAndConverter = Cast.unchecked( bdv.getViewer().state().getSources().get( setupId ) );
		RandomAccessibleInterval< T > inputRai = sourceAndConverter.getSpimSource().getSource( timePoint, level );
		if ( !isFloatType( inputRai ) )
			inputRai = convertToFloatType( inputRai );

		System.out.println( "Input Dimensions: " + Arrays.toString( inputRai.dimensionsAsLongArray() ) );
		Stardist3D.installRequirements();
		Stardist3D stardist3D = Stardist3D.fromPretained( "StarDist Plant Nuclei 3D ResNet", false );
		RandomAccessibleInterval< T > predictionRai = stardist3D.predict( inputRai );
		System.out.println( "Output Dimensions: " + Arrays.toString( predictionRai.dimensionsAsLongArray() ) );

		RandomAccessibleIntervalSource< FloatType > predictionSource =
				new RandomAccessibleIntervalSource<>( Cast.unchecked( predictionRai ), new FloatType(), "Prediction" );

		BdvFunctions.show( predictionSource, BdvOptions.options() );
	}

	private static boolean isFloatType( RandomAccessibleInterval< ? > rai )
	{
		return Util.getTypeFromInterval( rai ) instanceof FloatType;
	}

	private static < T extends RealType< T > > RandomAccessibleInterval< T >
			convertToFloatType( RandomAccessibleInterval< T > input )
	{
		Converter< T, FloatType > converter = ( source, target ) -> target.setReal( source.getRealDouble() );
		return Cast.unchecked( Converters.convert( input, converter, new FloatType() ) );
	}

	private static < T extends NumericType< T >, F extends RandomAccessibleInterval< T > > RandomAccessible< T > scaleLabelImage(
			F input, double scaleX, double scaleY, double scaleZ )
	{
		InvertibleRealTransform scaleTransform = new Scale3D( scaleX, scaleY, scaleZ );
		return Views.raster( RealViews
				.transform( RealViews.transform( Views.interpolate( Views.extendZero( input ), null ), scaleTransform ), scaleTransform ) );
	}

}
