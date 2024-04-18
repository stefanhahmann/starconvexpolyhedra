package net.stefanhahmann.stardist3d.jdll;

import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.model.Stardist3D;
import io.bioimage.modelrunner.numpy.DecodeNumpy;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class StarDist3dJdll
{
	public static < T extends RealType< T > & NativeType< T > > void main( String[] args ) throws IOException, InterruptedException,
			RuntimeException, MambaInstallException,
			ModelSpecsException, LoadEngineException,
			RunModelException, ArchiveException,
			URISyntaxException, LoadModelException
	{
		Stardist3D.installRequirements();
		Stardist3D model = Stardist3D.fromPretained( "StarDist Plant Nuclei 3D ResNet", false );
		RandomAccessibleInterval< T > input = DecodeNumpy.retrieveImgLib2FromNpy(
				new File( "models" + File.separator + "stardist-3d-plant-nuclei" + File.separator + "test_input.npy" )
						.getAbsolutePath() );

		// input = Utils.transpose( input );
		input = Views.hyperSlice( input, 4, 0 );
		input = Views.hyperSlice( input, 0, 0 );
		RandomAccessibleInterval< T > prediction = model.predict( input );
		BdvFunctions.show( input, "StarDist 3D input" ).setDisplayRange( 0, 200 );
		BdvFunctions.show( prediction, "StarDist 3D result" ).setDisplayRange( 0, 100 );

		System.out.println( true );
	}
}
