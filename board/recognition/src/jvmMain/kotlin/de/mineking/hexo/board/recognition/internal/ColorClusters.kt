package de.mineking.hexo.board.recognition.internal

import boofcv.alg.color.ColorDifference.deltaECIEDE2000
import org.ddogleg.clustering.ConfigKMeans
import org.ddogleg.clustering.FactoryClustering
import org.ddogleg.clustering.misc.ListAccessor

private const val MAX_COLOR_CLUSTERS = 128
private const val KMEANS_RESTART_LIMIT = 3
private const val CLUSTERING_RANDOM_SEED = 0L

internal class ColorClusters(
    val centers: List<LabColor>,
    /** One cluster index for each input color, in input order. */
    val assignments: IntArray,
)

/** Try increasing cluster counts until all colors fit their centers, or the search limit is reached. */
internal fun clusterColors(colors: List<LabColor>, tolerance: Double): ColorClusters {
    require(colors.isNotEmpty())
    val vectors = colors.map { doubleArrayOf(it.lightness, it.a, it.b) }
    val points = ListAccessor(vectors, { source, target -> source.copyInto(target) }, DoubleArray::class.java)
    val configuration = ConfigKMeans().apply { maxReSeed = KMEANS_RESTART_LIMIT }
    val kmeans = FactoryClustering.kMeans(configuration, 3, DoubleArray::class.java)
    val clusterLimit = minOf(colors.size, MAX_COLOR_CLUSTERS)

    // Keeping extra clusters lets classification reject an annotation instead of forcing it into X or O.
    for (clusterCount in 1..clusterLimit) {
        kmeans.initialize(CLUSTERING_RANDOM_SEED)
        kmeans.process(points, clusterCount)
        val allColorsFit = vectors.indices.all { index ->
            val center = kmeans.bestClusters[kmeans.assignments[index]]
            deltaECIEDE2000(vectors[index], center) <= tolerance
        }
        if (allColorsFit) break
    }

    val centers = kmeans.bestClusters.toList().map { LabColor(it[0], it[1], it[2]) }
    return ColorClusters(centers, kmeans.assignments.toArray())
}
