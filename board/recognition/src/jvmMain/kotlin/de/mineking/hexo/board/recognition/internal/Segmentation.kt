package de.mineking.hexo.board.recognition.internal

import boofcv.alg.misc.ImageStatistics
import boofcv.factory.segmentation.ConfigFh04
import boofcv.factory.segmentation.FactoryImageSegmentation
import boofcv.factory.segmentation.FactorySegmentationAlg
import boofcv.struct.feature.ColorQueue_F32
import boofcv.struct.image.GrayF32
import boofcv.struct.image.GrayS32
import boofcv.struct.image.ImageType
import boofcv.struct.image.Planar
import org.ddogleg.struct.DogArray_I32

// Graph-segmentation settings: prefer coherent interiors without merging across cell/stone edges.
private const val SEGMENTATION_SCALE = 40f
private const val MINIMUM_REGION_PIXELS = 10

internal data class ImageRegion(
    val pixelCount: Int,
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
    val meanColor: LabColor,
) {
    val width get() = maxX - minX + 1
    val height get() = maxY - minY + 1
}

internal data class ImageSegmentation(
    /** Pixel values index regions. These labels are not palette indices. */
    val regionLabels: GrayS32,
    val regions: List<ImageRegion>,
)

internal fun segmentImage(image: PreparedImage): ImageSegmentation {
    val imageType = ImageType.pl(3, GrayF32::class.java)
    val configuration = ConfigFh04(SEGMENTATION_SCALE, MINIMUM_REGION_PIXELS)
    val segmenter = FactoryImageSegmentation.fh04(configuration, imageType)

    val labels = GrayS32(image.lab.width, image.lab.height)
    segmenter.segment(image.lab, labels)

    val pixelCounts = countRegionPixels(labels, segmenter.totalSuperpixels)
    val meanColors = measureRegionColors(image.lab, labels, pixelCounts)

    val bounds = measureRegionBounds(labels, pixelCounts.size)
    val regions = List(pixelCounts.size) { index ->
        bounds[index].toImageRegion(pixelCounts[index], meanColors[index])
    }

    return ImageSegmentation(labels, regions)
}

private fun countRegionPixels(labels: GrayS32, regionCount: Int): DogArray_I32 {
    val counts = DogArray_I32().apply { resize(regionCount) }
    ImageStatistics.histogram(labels, 0, counts.data)
    return counts
}

private fun measureRegionColors(image: Planar<GrayF32>, labels: GrayS32, pixelCounts: DogArray_I32): List<LabColor> {
    val imageType = ImageType.pl(3, GrayF32::class.java)
    val means = ColorQueue_F32(3).apply { resize(pixelCounts.size) }

    FactorySegmentationAlg.regionMeanColor(imageType).process(image, labels, pixelCounts, means)
    return List(pixelCounts.size) { index ->
        val mean = means[index]
        LabColor(mean[0].toDouble(), mean[1].toDouble(), mean[2].toDouble())
    }
}

private fun measureRegionBounds(labels: GrayS32, regionCount: Int): List<RegionBounds> {
    val bounds = List(regionCount) { RegionBounds() }
    for (y in 0 until labels.height) {
        for (x in 0 until labels.width) {
            val regionIndex = labels[x, y]
            bounds[regionIndex].include(x, y)
        }
    }
    return bounds
}

private class RegionBounds {
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = 0
    var maxY = 0

    fun toImageRegion(pixelCount: Int, meanColor: LabColor) = ImageRegion(
        pixelCount = pixelCount,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        meanColor = meanColor,
    )

    fun include(x: Int, y: Int) {
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x)
        maxY = maxOf(maxY, y)
    }
}
