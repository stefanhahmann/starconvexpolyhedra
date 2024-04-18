package net.stefanhahmann.stardist3d;

import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.engine.installation.EngineInstall;
import io.bioimage.modelrunner.model.Model;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;
import io.bioimage.modelrunner.versionmanagement.InstalledEngines;
import io.scif.img.ImgOpener;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import net.stefanhahmann.polyhedron.StarConvexPolyhedron;
import net.stefanhahmann.regression.LinearRegression;
import org.ejml.simple.SimpleMatrix;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class demonstrates prediction of star-convex shapes in 3D using a pre-trained StarDist model.
 * <br>
 * The post processing of the predicted shapes is not yet fully implemented. Star convex do exist. Non maximum suppression (NMS) is yet required to reduce the number of shapes.
 * <br>
 * Requires:
 * <ul>
 *     <li>The <a href="https://www.tensorflow.org/install/source#gpu">requirements</a> for the latest supported tensorflow1 engine (=tensorflow_gpu version 1.15.0), i.e.</li>
 *     <li>
 *         <ul>
 *            <li>Installed <a href="https://developer.nvidia.com/cuda-toolkit-archive">cuda toolkit (10.0)</a></li>
 *            <li>Installed <a href="https://developer.nvidia.com/rdp/cudnn-archive">cuDNN (7.4)</a></li>
 *       </ul>
 *     </li>
 * </ul>
 */
public class StarDist3DDemo
{

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	private static final String ENGINE = EngineInfo.getBioimageioTfKey();

	// private static final String ENGINE = EngineInfo.getBioimageioPytorchKey(); // torchscript

	private static final String VERSION = "1.15.0";
	// private static final String VERSION = "2.0.0";

	private static final String ENGINE_DIRECTORY = "engines";

	private static final String MODEL_DIRECTORY = "models" + File.separator + "stardist-tensorflow";

	private static final String DATA_DIRECTORY = "data" + File.separator + "stardist";

	private static final boolean CPU = true;

	private static final boolean GPU = false;

	public static void main( String[] args ) throws Exception
	{
		EngineInfo engineInfo = setupEngine();
		Model model = Model.createDeepLearningModel( MODEL_DIRECTORY, MODEL_DIRECTORY, engineInfo );
		// AvailableEngines.getForCurrentOS().getVersions().forEach( System.out::println );
		RandomAccessibleInterval< ? extends RealType< ? > > image = loadImage( DATA_DIRECTORY + File.separator + "stardist_single.xml" );
		System.out.println( "imageShape: " + longArrayToString( image.dimensionsAsLongArray() ) );
		// TODO: Create a lower resolution image to speed up the computation
		// https://openaccess.thecvf.com/content_WACV_2020/papers/Weigert_Star-convex_Polyhedra_for_3D_Object_Detection_and_Segmentation_in_Microscopy_WACV_2020_paper.pdf
		// Section 2.2.: "To save computation and memory we predict at a grid of lower spatial resolution than the input image, since a dense (i.e., per input pixel) output is often not necessary."
		// cf.: https://github.com/stardist/stardist/blob/master/stardist/models/model3d.py#L123
		// Subsampling factors (must be powers of 2) for each of the axes. Model will predict on a subsampled grid for increased efficiency and larger field of view.
		Tensor< ? > prediction = processImage( image, model );

		RandomAccessibleInterval< FloatType > distances = getDistances( Cast.unchecked( prediction ) );
		RandomAccessibleInterval< FloatType > probabilities = getProbabilities( Cast.unchecked( prediction ) );
		StarDist3D starDist3D = new StarDist3D( distances, probabilities );
		// TODO: Implement non-maximum suppression (NMS) to reduce the number of shapes
		// computeEllipsoids( starDist );
	}

	private static Tensor< ? > processImage( RandomAccessibleInterval< ? extends RealType< ? > > image, Model model )
			throws Exception
	{
		String axes = "xyzbc";
		List< Tensor< ? > > inputTensors = getInputTensors( image, axes );
		List< Tensor< ? > > outputTensors = Collections.singletonList( Tensor.buildEmptyTensor( "output", axes ) );

		model.loadModel();
		model.runModel( inputTensors, outputTensors );
		model.closeModel();

		return outputTensors.get( 0 );
	}

	private static EngineInfo setupEngine()
	{
		List< DeepLearningVersion > installedEngines =
				InstalledEngines.checkEngineWithArgsInstalledForOS( ENGINE, VERSION, CPU, GPU, ENGINE_DIRECTORY );
		if ( installedEngines.isEmpty() )
			installEngine();

		EngineInfo engineInfo = EngineInfo.defineDLEngine( ENGINE, VERSION, ENGINE_DIRECTORY );
		if ( engineInfo == null )
			throw new RuntimeException( "Engine not properly installed: " + ENGINE + ", version: '"
					+ VERSION + "', directory: '" + ENGINE_DIRECTORY + "'" );
		return engineInfo;
	}

	private static List< Tensor< ? > > getInputTensors( final RandomAccessibleInterval< ? extends RealType< ? > > image, final String axes )
	{
		// set dimensions and 2 to the previous 3 (5 in total)
		// data dimension (probability vs distance)
		// ray dimension
		Img< FloatType > data = setDimensions( image, 5 );
		List< Tensor< ? > > inputTensors = new ArrayList<>();
		inputTensors.add( Tensor.build( "input", axes, data ) );
		return inputTensors;
	}

	private static Img< FloatType > setDimensions( RandomAccessibleInterval< ? extends RealType< ? > > image, int numDimensions )
	{
		long[] newDimensions = new long[ numDimensions ];
		for ( int i = 0; i < image.numDimensions(); i++ )
			newDimensions[ i ] = image.dimension( i );
		for ( int i = image.numDimensions(); i < numDimensions; i++ )
			newDimensions[ i ] = 1;

		Img< FloatType > data = ArrayImgs.floats( newDimensions );

		// copy image data into data after the dimensions have been added
		Cursor< FloatType > cursor = data.localizingCursor();
		RandomAccess< ? extends RealType< ? extends RealType< ? > > > source = image.randomAccess();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.get().set( source.setPositionAndGet( cursor ).getRealFloat() );
		}
		return data;
	}

	private static void installEngine()
	{
		try
		{
			EngineInstall.installEngineWithArgsInDir( ENGINE, VERSION, CPU, GPU, ENGINE_DIRECTORY );
		}
		catch ( IOException | InterruptedException e )
		{
			logger.error(
					"Could not install engine (name: '{}' version: '{}' into: {}). Message: {}", ENGINE,
					VERSION, new File( ENGINE_DIRECTORY ).getAbsolutePath(), e.getMessage()
			);
		}

	}

	@SuppressWarnings( "SameParameterValue" )
	private static RandomAccessibleInterval< ? extends RealType< ? > > loadImage( String location )
	{
		if ( location.endsWith( ".xml" ) )
		{
			File xmlFile = new File( location );
			try
			{
				Element elem = new SAXBuilder().build( xmlFile ).getRootElement();
				SpimData spimData = new XmlIoSpimData().fromXml( elem, xmlFile );
				return Cast.unchecked( spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 )
						.getImage( 0 ) );
			}
			catch ( IOException | JDOMException | SpimDataException e )
			{
				throw new RuntimeException( e );
			}
		}
		else
		{
			return new ImgOpener().openImgs( location, new UnsignedShortType() ).get( 0 );
		}
	}

	private static RandomAccessibleInterval< FloatType > getProbabilities( Tensor< FloatType > prediction )
	{
		return Views.hyperSlice( prediction.getData(), 3, 0 );
	}

	private static RandomAccessibleInterval< FloatType > getDistances( Tensor< FloatType > prediction )
	{
		long[] tensorShape = Arrays.stream( prediction.getShape() ).asLongStream().toArray();
		logger.debug( "tensorShape: {}", longArrayToString( tensorShape ) );
		// remove the ray dimension
		tensorShape[ 4 ]--;
		return Views.offsetInterval( prediction.getData(), new long[] { 0, 0, 0, 1, 0 }, tensorShape );
	}

	private static String longArrayToString( long[] longArray )
	{
		return Arrays.stream( longArray ).mapToObj( String::valueOf ).collect( Collectors.joining( ", " ) );
	}

	private static void computeEllipsoids( StarDist3D starDist3D )
	{
		List< SimpleMatrix > ellipsoids = new ArrayList<>();
		for ( StarConvexPolyhedron surface : starDist3D.getStarConvexPolyhedra() )
		{
			List< double[] > points = surface.getPoints();
			SimpleMatrix lrX = new SimpleMatrix( points.size(), 9 ); // 9 parameters are required to describe an ellipsoid.
			SimpleMatrix lrY = new SimpleMatrix( points.size(), 1 );
			lrY.set( 1 ); // normalization for the ellipsoid eq.

			for ( int row = 0; row < points.size(); row++ )
			{
				double[] point = points.get( row );
				double x = point[ 0 ];
				double y = point[ 1 ];
				double z = point[ 2 ];

				lrX.set( row, 0, x * x );
				lrX.set( row, 1, y * y );
				lrX.set( row, 2, z * z );
				lrX.set( row, 3, x * y );
				lrX.set( row, 4, x * z );
				lrX.set( row, 5, y * z );
				lrX.set( row, 6, x );
				lrX.set( row, 7, y );
				lrX.set( row, 8, z );
			}
			// ax^2 + by^2 + cz^2 + dxy + exz - fyz + gx + hy - iz - 1 == 0
			SimpleMatrix ellipsoid = LinearRegression.fit( lrX, lrY ).transpose();
			ellipsoids.add( ellipsoid );
		}
		ellipsoids.forEach( simpleMatrix -> logger.debug( "ellpsoid: {}", simpleMatrix ) );
	}
}
