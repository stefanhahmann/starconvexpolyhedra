package net.stefanhahmann.polyhedron.kotlin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.stream.Collectors

internal class StarConvexPolyhedronTest {
    private lateinit var unitPolyhedronAtZero: StarConvexPolyhedron

    private lateinit var tinyPolyhedronAtZero: StarConvexPolyhedron

    private lateinit var bigPolyhedronAt50: StarConvexPolyhedron

    private val centerZero = doubleArrayOf(0.0, 0.0, 0.0)

    private var center50: DoubleArray = doubleArrayOf(50.0, 50.0, 50.0)

    private lateinit var pointInsideTinyPolyhedron: DoubleArray

    private lateinit var pointOutSideTinyPolyhedron: DoubleArray

    private var shouldBeOutside: DoubleArray = doubleArrayOf(25.0, 12.0, 50.0)

    private val random = Random()

    @BeforeEach
    fun setUp() {
        random.setSeed(1)

        var distances: List<Double>? = ArrayList(Collections.nCopies(96, 1.0))
        unitPolyhedronAtZero = StarConvexPolyhedron(centerZero, distances)

        distances = random.doubles(96, 2.0, 5.0)
            .boxed()
            .collect(Collectors.toList())
        tinyPolyhedronAtZero = StarConvexPolyhedron(centerZero, distances)
        distances = random.doubles(96, 5.0, 40.0)
            .boxed()
            .collect(Collectors.toList())
        bigPolyhedronAt50 = StarConvexPolyhedron(center50, distances)
        pointInsideTinyPolyhedron = doubleArrayOf(1.0, 1.0, 1.0)
        pointOutSideTinyPolyhedron = doubleArrayOf(10.0, 20.0, 15.0)
    }

    @Test
    fun testContains1() {
        Assertions.assertTrue(tinyPolyhedronAtZero.contains(centerZero))
        Assertions.assertTrue(tinyPolyhedronAtZero.contains(pointInsideTinyPolyhedron))
        Assertions.assertFalse(tinyPolyhedronAtZero.contains(pointOutSideTinyPolyhedron))
    }

    @Test
    fun testContains() {
        random.setSeed(1)
        Assertions.assertTrue(bigPolyhedronAt50.contains(center50))
        Assertions.assertFalse(bigPolyhedronAt50.contains(shouldBeOutside))
    }

    @Test
    fun testGetBoundingBox3D() {
        val boundingBox3D = tinyPolyhedronAtZero.boundingBox3D
        Assertions.assertTrue(boundingBox3D.contains(centerZero))
        Assertions.assertTrue(boundingBox3D.contains(pointInsideTinyPolyhedron))
        Assertions.assertFalse(boundingBox3D.contains(pointOutSideTinyPolyhedron))
    }
}
