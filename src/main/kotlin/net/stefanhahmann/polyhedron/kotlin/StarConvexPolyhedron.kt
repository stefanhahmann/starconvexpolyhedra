package net.stefanhahmann.polyhedron.kotlin

import cn.jimmiez.pcu.common.graphics.Octree
import net.imglib2.util.LinAlgHelpers
import net.stefanhahmann.polyhedron.FibonacciLattices
import java.util.function.Consumer
import javax.vecmath.Point3d
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Class to generate a star convex polyhedron.
 *
 * @see [Star-convex Polyhedra for 3D Object Detection and Segmentation in Microscopy](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435)
 *
 * @see [Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435)
 *
 * @author Stefan Hahmann
 */
class StarConvexPolyhedron {
    val lattice: List<DoubleArray>

    val center: DoubleArray

    val points: List<DoubleArray>

    val boundingBox3D: BoundingBox3D

    private val octree: Octree

    /**
     * Creates a star convex polyhedron with the given center and distances to the points. The number of points that the polyhedron contains is determined by the number of given distances.
     * @param center the center of the polyhedron. Must not be null. Expected order: xyz.
     * @param distances the distances from the center to the points. Must not be null. Must contain at least 4 distances.
     */
    constructor(center: DoubleArray?, distances: List<Double?>?) {
        requireNotNull(center) { "center cannot be null." }
        requireNotNull(distances) { "distances cannot be null." }
        require(distances.isNotEmpty()) { "distances cannot be empty." }
        val nPoints = distances.size
        if (nPoints == DEFAULT_SIZE) this.lattice = DEFAULT_LATTICE
        else this.lattice = FibonacciLattices.getValues(nPoints)
        require(nPoints >= 4) { "At least 4 distances are required." }
        octree = Octree()
        initOctree()
        this.center = center
        this.points = ArrayList()
        for (i in 0 until nPoints) {
            val point = DoubleArray(3)
            // TODO allow to correct for anisotropy here
            LinAlgHelpers.scale(lattice[i], distances[i]!!, point)
            LinAlgHelpers.add(center, point, point)
            points += point
        }

        val minMax = minMax()
        val min = doubleArrayOf(minMax[0], minMax[1], minMax[2])
        val max = doubleArrayOf(minMax[3], minMax[4], minMax[5])
        this.boundingBox3D = BoundingBox3D(min, max)
    }

    private fun initOctree() {
        val latticePoints: MutableList<Point3d> = ArrayList()
        lattice.forEach(Consumer { point: DoubleArray -> latticePoints.add(Point3d(point[0], point[1], point[2])) })
        octree.buildIndex(latticePoints)
    }

    internal constructor(
        center: DoubleArray,
        vertices: List<DoubleArray>,
        min: DoubleArray?,
        max: DoubleArray?,
        lattice: List<DoubleArray>
    ) {
        this.center = center
        this.lattice = lattice
        this.points = vertices.toMutableList()
        this.boundingBox3D = BoundingBox3D(min, max)
        this.octree = Octree()
        initOctree()
    }

    /**
     * Tests if the given point is inside the star convex polyhedron and returns true if it is.
     *
     *
     * Workflow:
     *
     *  * Project given point on unit sphere
     *  *
     *
     *  1. Subtract center from point
     *  1. Normalize point
     *  1. Point is now on unit sphere
     *
     *
     *  * Find the 3 nearest points to this point on the unit sphere
     *  * Construct a triangle from these 3 points
     *  * Test on which side of the triangle the point lies
     *  * If the point lies on the same side as the center, it is inside the polyhedron
     *
     * @param point the point to test. Must not be null.
     * @return true if the given point is inside the star convex polyhedron.
     */
    fun contains(point: DoubleArray?): Boolean {
        requireNotNull(point) { "Point cannot be null." }
        if (point.contentEquals(center)) return true
        val nearestPoints = findNearestPoints(point)
        return sideOfTriangle(point, nearestPoints) == sideOfTriangle(center, nearestPoints)
    }

    private fun findNearestPoints(candidate: DoubleArray?): List<DoubleArray> {
        val copy = DoubleArray(3)
        // project on unit sphere
        LinAlgHelpers.subtract(candidate, center, copy)
        LinAlgHelpers.normalize(copy)

        val indices = octree.searchNearestNeighbors(3, Point3d(copy[0], copy[1], copy[2]))

        val result: MutableList<DoubleArray> = ArrayList()
        for (i in 0..2) {
            result.add(points[indices[i]])
        }
        return result
    }

    private fun minMax(): DoubleArray {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE
        var maxZ = Double.MIN_VALUE

        for (vector in points) {
            val x = vector[0]
            val y = vector[1]
            val z = vector[2]

            minX = min(minX, x)
            minY = min(minY, y)
            minZ = min(minZ, z)

            maxX = max(maxX, x)
            maxY = max(maxY, y)
            maxZ = max(maxZ, z)
        }

        return doubleArrayOf(minX, minY, minZ, maxX, maxY, maxZ)
    }

    inner class BoundingBox3D
        (val minPoint: DoubleArray?, val maxPoint: DoubleArray?) {
        fun contains(point: DoubleArray?): Boolean {
            requireNotNull(point) { "Point cannot be null." }
            if (point.contentEquals(minPoint) || point.contentEquals(maxPoint)) return true
            if (point.contentEquals(center)) return true
            // Check if the given 3D point is inside the bounding box.
            return point[0] >= minPoint!![0] && point[0] <= maxPoint!![0] && point[1] >= minPoint[1] && point[1] <= maxPoint[1] && point[2] >= minPoint[2] && point[2] <= maxPoint[2]
        }
    }

    companion object {
        private const val DEFAULT_SIZE = 96

        private val DEFAULT_LATTICE: List<DoubleArray> = FibonacciLattices.getValues(DEFAULT_SIZE)

        private fun sideOfTriangle(point: DoubleArray, triangle: List<DoubleArray>): Byte {
            require(triangle.size == 3) { "Triangle must have 3 vertices, but has: " + triangle.size }
            val point1 = triangle[0]
            val point2 = triangle[1]
            val point3 = triangle[2]

            val diff21 = DoubleArray(3)
            LinAlgHelpers.subtract(point2, point1, diff21)
            val diff31 = DoubleArray(3)
            LinAlgHelpers.subtract(point3, point1, diff31)

            val cross = DoubleArray(3)
            LinAlgHelpers.cross(diff21, diff31, cross)
            LinAlgHelpers.normalize(cross)
            val toTestPoint = DoubleArray(3)
            LinAlgHelpers.subtract(point, point1, toTestPoint)
            val dotProduct = LinAlgHelpers.dot(cross, toTestPoint)
            return sign(dotProduct).toInt().toByte()
        }
    }
}
