package net.stefanhahmann.regression;

import org.ejml.factory.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;

public class LinearRegression
{
	private LinearRegression()
	{
		// prevent instantiation
	}

	public static SimpleMatrix fit( SimpleMatrix x, SimpleMatrix y )
	{
		try
		{
			return x.transpose().mult( x ).invert().mult( x.transpose() ).mult( y );
		}
		catch ( SingularMatrixException e )
		{
			System.out.println( "[WARN] not enough points to determine regression! Need at least " + x.numRows() );
			throw e;
		}
	}
}
