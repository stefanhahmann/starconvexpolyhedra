package net.stefanhahmann.polyhedron.kotlin

import bdv.viewer.Source
import net.imglib2.Cursor
import net.imglib2.IterableInterval
import net.imglib2.Localizable
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.roi.BoundaryType
import net.imglib2.roi.KnownConstant
import net.imglib2.roi.Regions
import net.imglib2.roi.mask.integer.DefaultMask
import net.imglib2.util.Intervals
import net.imglib2.view.Views
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.ceil
import kotlin.math.floor

class StarConvexPolyhedronIterable<T>(private val source: Source<T>) : IterableInterval<T>, Localizable {
    /**
     * transform of current source to global coordinates
     */
    private val sourceTransform = AffineTransform3D()

    private var polyhedronVoxels: IterableInterval<T>? = null

    /**
     * position of a single voxel in source coordinates
     */
    private val tempVoxel = DoubleArray(3)

    /*
	 * center of polyhedron in source coordinates
	 */
    private var center: DoubleArray = DoubleArray(3)

    /**
     * bounding box min
     */
    private var minPoint: DoubleArray = DoubleArray(3)

    /**
     * bounding box max
     */
    private var maxPoint: DoubleArray = DoubleArray(3)

    /**
     * bounding box min
     */
    // TODO why is this long?
    private val min = LongArray(3)

    /**
     * bounding box max
     */
    private val max = LongArray(3)

    override fun cursor(): Cursor<T> {
        return polyhedronVoxels!!.cursor()
    }

    override fun localizingCursor(): Cursor<T> {
        return polyhedronVoxels!!.localizingCursor()
    }

    override fun size(): Long {
        return polyhedronVoxels!!.size()
    }

    override fun firstElement(): T {
        return polyhedronVoxels!!.firstElement()
    }

    override fun iterationOrder(): Any {
        return polyhedronVoxels!!.iterationOrder()
    }

    override fun iterator(): MutableIterator<T> {
        return polyhedronVoxels!!.iterator()
    }

    override fun min(d: Int): Long {
        return min[d]
    }

    override fun max(d: Int): Long {
        return max[d]
    }

    override fun getLongPosition(d: Int): Long {
        return Math.round(center[d])
    }

    override fun numDimensions(): Int {
        return 3
    }

    /**
     * Resets this iterable to that it iterates over the specified polyhedron, at the
     * specified resolution level in the source. Generate an error of the
     * specified resolution level is not present in the source.
     *
     * @param polyhedron
     * the polyhedron to iterate.
     * @param resolutionLevel
     * the resolution level to use in the source.
     */
    @JvmOverloads
    fun reset(polyhedron: StarConvexPolyhedron, timepoint: Int, resolutionLevel: Int = 0) {
        // get source transform from source
        source.getSourceTransform(timepoint, resolutionLevel, sourceTransform)
        val img = source.getSource(timepoint, resolutionLevel)

        // transform spot position into source coordinates
        sourceTransform.inverse().apply(polyhedron.center, center)

        // transform polyhedron vertices into source coordinates
        val vertices: List<DoubleArray> = ArrayList(polyhedron.points)
        vertices.forEach(Consumer { vertex: DoubleArray? -> sourceTransform.inverse().apply(vertex, vertex) })

        // transform bounding box into source coordinates
        sourceTransform.inverse().apply(polyhedron.boundingBox3D.minPoint, minPoint)
        sourceTransform.inverse().apply(polyhedron.boundingBox3D.maxPoint, maxPoint)

        // transform lattice on unit sphere into source coordinates
        val lattice = polyhedron.lattice
        lattice.forEach(Consumer { vertex: DoubleArray? -> sourceTransform.inverse().apply(vertex, vertex) })

        // get transformed bounding box with long coordinates
        for (d in 0..2) {
            min[d] = kotlin.math.max(0.0, floor(minPoint[d]).toLong().toDouble()).toLong()
            max[d] = kotlin.math.min(img.max(d).toDouble(), ceil(maxPoint[d]).toLong().toDouble())
                .toLong()
        }
        logger.debug(
            "bounding volume of polyhedra defined by min ({},{},{}) and max ({},{},{}).", minPoint[0], minPoint[1],
            minPoint[2], maxPoint[0], maxPoint[1], maxPoint[2]
        )
        logger.debug(
            "bounding box of polyhedra in source coordinates defined by min ({},{},{}) and max ({},{},{}).",
            min[0],
            min[1],
            min[2],
            max[0],
            max[1],
            max[2]
        )
        logger.debug(
            "bounding volume, length in x direction: {}, length in y direction: {}, length in z direction: {}",
            max[0] - min[0], max[1] - min[1], max[2] - min[2]
        )
        logger.debug("bounding volume size = {}", (max[0] - min[0]) * (max[1] - min[1]) * (max[2] - min[2]))

        // if bounding box is empty, we set it to cover pixel at (0,0,0)
        // this will hopefully not cause problems, because it would not overlap
        // with polyhedra, so the polyhedra iterable could be empty.
        if (Intervals.isEmpty(this)) for (d in 0..2) {
            max[d] = 0
            min[d] = 0
        }

        // create a new polyhedron with the vertices transformed into source coordinates
        val transformedPolyhedron = StarConvexPolyhedron(center, vertices, minPoint, maxPoint, lattice)

        // inflate polyhedra by .5 pixels on either side
        // TODO
        val count = AtomicInteger(0)
        // create mask
        val contains = Predicate<Localizable> { localizable: Localizable ->
            count.incrementAndGet()
            if (count.get() % 100000 == 0) logger.debug("contains count = {}", count.get())
            localizable.localize(tempVoxel)
            transformedPolyhedron.contains(tempVoxel)
        }
        val mask = DefaultMask(3, BoundaryType.UNSPECIFIED, contains, KnownConstant.UNKNOWN)
        polyhedronVoxels = Regions.sampleWithMask(mask, Views.interval(img, this))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
