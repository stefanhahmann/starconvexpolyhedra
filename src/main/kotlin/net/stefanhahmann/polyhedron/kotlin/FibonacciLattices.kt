package net.stefanhahmann.polyhedron.kotlin

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

typealias Points = List<DoubleArray>

/**
 * Class to compute the points of a spherical Fibonacci lattice
 *
 * @see [Star-convex Polyhedra for 3D Object Detection and Segmentation in Microscopy](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435)
 *
 * @see [Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435)
 *
 * @author Stefan Hahmann
 */
object FibonacciLattices {
    private val PHI = (1 + sqrt(5.0)) / 2

    /**
     * Returns the points of a spherical Fibonacci lattice with n points
     *
     * @param n number of points
     * @return points of a spherical Fibonacci lattice with n points. Order: zyx.
     * @see [Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435) Section 2.1, 3656
     */
    fun getValues(n: Int): Points {
        return List(n) { k ->
            val zyx = getZYX(k.toDouble(), n.toDouble())
            doubleArrayOf(zyx[2], zyx[1], zyx[0])
        }
    }

    fun getZYX(k: Double, n: Double): DoubleArray {
        val z = -1 + 2.0 * k / (n - 1)
        val angle = 2 * Math.PI * (1 - 1 / PHI) * k
        fun sqrtZ() = sqrt(1 - z * z)
        return doubleArrayOf(z, sqrtZ() * sin(angle), sqrtZ() * cos(angle))
    }

}
