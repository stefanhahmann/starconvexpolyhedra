package net.stefanhahmann.stardist3d.jdll;

import java.io.IOException;
import java.net.URISyntaxException;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

import org.apache.commons.compress.archivers.ArchiveException;

import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.model.Stardist3D;

/**
 * Predict with a dummy image using the StarDist 3D model.<br>
 * The purpose of this class is to check if the model can be loaded and run without any issues.<br>
 * Works with JDLL Release 0.5.8
 */
public class StarDist3DDummyPrediction
{
	public static void main( String[] args )
			throws MambaInstallException, IOException, URISyntaxException, ArchiveException, InterruptedException, ModelSpecsException,
			LoadEngineException, RunModelException, LoadModelException
	{
		Stardist3D.installRequirements();
		Stardist3D stardist3D = Stardist3D.fromPretained( "StarDist Plant Nuclei 3D ResNet", false );
		final Img< FloatType > dummyImg = ArrayImgs.floats( 10, 10, 10 );
		stardist3D.predict( dummyImg );
	}
}
