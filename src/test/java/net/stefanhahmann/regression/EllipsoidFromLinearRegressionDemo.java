package net.stefanhahmann.regression;

import org.ejml.simple.SimpleMatrix;

import java.util.Random;

public class EllipsoidFromLinearRegressionDemo
{
	public static void main( String[] args )
	{
		doFit();
	}

	private static String withOp( double in )
	{
		if ( in < 0 )
		{
			return String.format( "- %.3f", -in );
		}
		else
		{
			return String.format( "+ %.3f", in );
		}
	}

	private static void doFit()
	{
		int n = 12;
		SimpleMatrix points = SimpleMatrix.random( n, 3, -1, 1, new Random() );
		SimpleMatrix lrX = new SimpleMatrix( n, 9 );

		for ( int row = 0; row < n; row++ )
		{

			double x = points.get( row, 0 );
			double y = points.get( row, 1 );
			double z = points.get( row, 2 );

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
		SimpleMatrix lrY = new SimpleMatrix( n, 1 );
		lrY.set( 1 );

		SimpleMatrix parameters = LinearRegression.fit( lrX, lrY );

		System.out.printf( "%.3fx^2 %sy^2 %sz^2 %sx y %sx z %sy z %sx %sy %sz - 1 == 0",
				parameters.get( 0 ), withOp( parameters.get( 1 ) ), withOp( parameters.get( 2 ) ),
				withOp( parameters.get( 3 ) ), withOp( parameters.get( 4 ) ), withOp( parameters.get( 5 ) ),
				withOp( parameters.get( 6 ) ), withOp( parameters.get( 7 ) ), withOp( parameters.get( 8 ) )
		);
	}
}
