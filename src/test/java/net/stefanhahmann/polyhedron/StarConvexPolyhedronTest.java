package net.stefanhahmann.polyhedron;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarConvexPolyhedronTest
{
	StarConvexPolyhedron unitPolyhedronAtZero;

	StarConvexPolyhedron tinyPolyhedronAtZero;

	StarConvexPolyhedron bigPolyhedronAt50;

	private final double[] centerZero = new double[] { 0, 0, 0 };

	double[] center50 = new double[] { 50, 50, 50 };

	private double[] pointInsideTinyPolyhedron;

	private double[] pointOutSideTinyPolyhedron;

	double[] shouldBeOutside = new double[] { 25, 12, 50 };

	private final Random random = new Random();

	@BeforeEach
	public void setUp()
	{
		random.setSeed( 1 );

		List< Double > distances = new ArrayList<>( Collections.nCopies( 96, 1d ) );
		unitPolyhedronAtZero = new StarConvexPolyhedron( centerZero, distances );

		distances = random.doubles( 96, 2, 5 )
				.boxed()
				.collect( Collectors.toList() );
		tinyPolyhedronAtZero = new StarConvexPolyhedron( centerZero, distances );
		distances = random.doubles( 96, 5, 40 )
				.boxed()
				.collect( Collectors.toList() );
		bigPolyhedronAt50 = new StarConvexPolyhedron( center50, distances );
		pointInsideTinyPolyhedron = new double[] { 1, 1, 1 };
		pointOutSideTinyPolyhedron = new double[] { 10, 20, 15 };
	}

	@Test
	void testContains1()
	{
		assertTrue( tinyPolyhedronAtZero.contains( centerZero ) );
		assertTrue( tinyPolyhedronAtZero.contains( pointInsideTinyPolyhedron ) );
		assertFalse( tinyPolyhedronAtZero.contains( pointOutSideTinyPolyhedron ) );
	}

	@Test
	void testContains()
	{
		random.setSeed( 1 );
		assertTrue( bigPolyhedronAt50.contains( center50 ) );
		assertFalse( bigPolyhedronAt50.contains( shouldBeOutside ) ); //
	}

	@Test
	void testGetBoundingBox3D()
	{
		StarConvexPolyhedron.BoundingBox3D boundingBox3D = tinyPolyhedronAtZero.getBoundingBox3D();
		assertTrue( boundingBox3D.contains( centerZero ) );
		assertTrue( boundingBox3D.contains( pointInsideTinyPolyhedron ) );
		assertFalse( boundingBox3D.contains( pointOutSideTinyPolyhedron ) );
	}
}
