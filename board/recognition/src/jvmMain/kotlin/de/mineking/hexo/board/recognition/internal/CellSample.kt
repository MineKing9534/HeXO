package de.mineking.hexo.board.recognition.internal

import boofcv.alg.misc.ImageMiscOps
import boofcv.alg.misc.ImageStatistics
import boofcv.struct.image.GrayS32
import boofcv.struct.image.GrayU8
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.recognition.RecognizedGrid
import kotlin.math.ceil
import kotlin.math.roundToInt

// Exclude central labels and outer grid strokes, retaining the colored interior of filled or outlined stones.
private const val INNER_SAMPLE_RADIUS_FRACTION = 0.19
private const val OUTER_SAMPLE_RADIUS_FRACTION = 0.40

internal class CellSample(
    val coordinate: CellCoordinate,
    /** Counts indexed by Palette.colors; masked-out pixels are not included. */
    val colorCounts: IntArray,
) {
    val pixelCount = colorCounts.sum()
    val dominantColorIndex = colorCounts.indices.maxBy { colorCounts[it] }
}

internal fun sampleCells(palette: Palette, grid: RecognizedGrid): List<CellSample> {
    val mask = createSamplingMask(grid.spacing)
    val windowSize = mask.width
    val windowRadius = windowSize / 2
    val window = GrayS32(windowSize, windowSize)
    val image = palette.colorIndices
    val samples = mutableListOf<CellSample>()
    val coordinates = grid.completeCellCoordinates(image.width, image.height, windowRadius)

    for (coordinate in coordinates) {
        val left = grid.centerX(coordinate).roundToInt() - windowRadius
        val top = grid.centerY(coordinate).roundToInt() - windowRadius
        ImageMiscOps.copy(left, top, 0, 0, windowSize, windowSize, image, window)

        // Reserve the final histogram bin for excluded pixels, then discard it.
        val excludedPixelIndex = palette.colors.size
        ImageMiscOps.maskFill(window, mask, 0, excludedPixelIndex)
        val histogram = IntArray(palette.colors.size + 1)
        ImageStatistics.histogram(window, 0, histogram)
        samples += CellSample(coordinate, histogram.copyOf(palette.colors.size))
    }
    return samples
}

private fun createSamplingMask(spacing: Double): GrayU8 {
    val innerRadius = spacing * INNER_SAMPLE_RADIUS_FRACTION
    val outerRadius = spacing * OUTER_SAMPLE_RADIUS_FRACTION
    val radius = ceil(outerRadius).toInt()
    val size = radius * 2 + 1
    val squaredRadiusRange = innerRadius * innerRadius..outerRadius * outerRadius
    val mask = GrayU8(size, size)
    for (index in mask.data.indices) {
        val dx = index % size - radius
        val dy = index / size - radius
        val squaredDistance = (dx * dx + dy * dy).toDouble()
        mask.data[index] = if (squaredDistance in squaredRadiusRange) 1 else 0
    }
    return mask
}
