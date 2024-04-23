package net.stefanhahmann.stardist3d.jdll;

import bdv.util.BdvFunctions;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.model.Stardist3D;
import io.bioimage.modelrunner.numpy.DecodeNumpy;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * This class demonstrates how to use a StarDist 3D model from the BioImage.io model zoo to predict on a 3D image.
 */
public class StarDist3dJdll
{
	private final static String MODEL_NAME = "StarDist Plant Nuclei 3D ResNet";

	private final static String TEST_DATA = "data" + File.separator + "stardist-3d-plant-nuclei" + File.separator + "test_input.npy";

	public static < T extends RealType< T > & NativeType< T > > void main( String[] args ) throws IOException, InterruptedException,
			RuntimeException, MambaInstallException,
			ModelSpecsException, LoadEngineException,
			RunModelException, ArchiveException,
			URISyntaxException, LoadModelException
	{
		Stardist3D.installRequirements();
		RandomAccessibleInterval< T > input = DecodeNumpy.retrieveImgLib2FromNpy( new File( TEST_DATA ).getAbsolutePath() );

		// remove singleton dimensions from input, i.e. remove time and channel dimension
		input = Views.hyperSlice( input, 4, 0 );
		input = Views.hyperSlice( input, 0, 0 );

		// input = Utils.transpose( input );
		// prediction
		Stardist3D model = Stardist3D.fromPretained( MODEL_NAME, false );
		RandomAccessibleInterval< T > prediction = model.predict( input );

		// show results in Bdv
		BdvFunctions.show( input, "StarDist 3D input" ).setDisplayRange( 0, 200 );
		BdvFunctions.show( prediction, "StarDist 3D result" ).setDisplayRange( 0, 100 );

		// show results in ImageJ
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.ui().show( input );
		ij.ui().show( prediction );
		RandomAccessibleInterval< UnsignedShortType > converted =
				Converters.convert( input, ( i, o ) -> o.setReal( i.getRealDouble() ), new UnsignedShortType() );
		ij.ui().show( converted );

	}
}
