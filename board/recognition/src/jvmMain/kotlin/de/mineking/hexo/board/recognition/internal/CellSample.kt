package de.mineking.hexo.board.recognition.internal

import boofcv.alg.misc.ImageMiscOps
import boofcv.alg.misc.ImageStatistics
import boofcv.struct.image.GrayS32
import boofcv.struct.image.GrayU8
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.recognition.RecognizedGrid
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Exclude central labels and outer grid strokes, retaining the colored interior of filled or outlined stones.
private const val INNER_SAMPLE_RADIUS_FRACTION = 0.19
private const val OUTER_SAMPLE_RADIUS_FRACTION = 0.40
private const val MINIMUM_VISIBLE_SAMPLE_FRACTION = 0.25
private val BACKGROUND_SAMPLE_RADII = doubleArrayOf(0.50, 0.54, 0.57)

internal class CellSample(
    val coordinate: CellCoordinate,
    /** Counts indexed by Palette.colors; masked-out pixels are not included. */
    val colorCounts: IntArray,
    val isComplete: Boolean = true,
) {
    val pixelCount = colorCounts.sum()
    val dominantColorIndex = colorCounts.indices.maxBy { colorCounts[it] }
}

internal class CellSampling(val samples: List<CellSample>, val exteriorColorCounts: IntArray)

internal fun sampleCells(palette: Palette, grid: RecognizedGrid): CellSampling {
    val mask = createSamplingMask(grid.spacing)
    val windowSize = mask.width
    val windowRadius = windowSize / 2
    val window = GrayS32(windowSize, windowSize)
    val image = palette.colorIndices
    val samples = mutableListOf<CellSample>()
    val exteriorCounts = IntArray(palette.colors.size)
    val excludedPixelIndex = palette.colors.size
    val fullPixelCount = mask.data.count { it.toInt() != 0 }
    val coordinates = grid.cellCoordinatesWithinMargin(image.width, image.height, -windowRadius)

    for (coordinate in coordinates) {
        val left = grid.centerX(coordinate).roundToInt() - windowRadius
        val top = grid.centerY(coordinate).roundToInt() - windowRadius
        copyVisiblePixels(image, window, left, top, excludedPixelIndex)
        sampleExterior(palette, grid, coordinate, exteriorCounts)

        // Reserve the final histogram bin for excluded pixels, then discard it.
        ImageMiscOps.maskFill(window, mask, 0, excludedPixelIndex)
        val histogram = IntArray(excludedPixelIndex + 1)
        ImageStatistics.histogram(window, 0, histogram)
        val sample = CellSample(
            coordinate, histogram.copyOf(excludedPixelIndex),
            left >= 0 && top >= 0 && left + windowSize <= image.width && top + windowSize <= image.height,
        )
        // Tiny edge slivers do not provide reliable cell appearance evidence.
        if (sample.pixelCount >= fullPixelCount * MINIMUM_VISIBLE_SAMPLE_FRACTION) samples += sample
    }
    return CellSampling(samples, exteriorCounts)
}

/** Fill clipped-away pixels with the histogram's excluded-color index before copying visible pixels. */
private fun copyVisiblePixels(
    image: GrayS32,
    window: GrayS32,
    left: Int,
    top: Int,
    excludedPixelIndex: Int,
) {
    val sourceLeft = left.coerceAtLeast(0)
    val sourceTop = top.coerceAtLeast(0)
    val sourceRight = (left + window.width).coerceAtMost(image.width)
    val sourceBottom = (top + window.height).coerceAtMost(image.height)
    ImageMiscOps.fill(window, excludedPixelIndex)
    ImageMiscOps.copy(
        sourceLeft, sourceTop, sourceLeft - left, sourceTop - top,
        sourceRight - sourceLeft, sourceBottom - sourceTop, image, window,
    )
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

/** Probe several offsets near hex vertices; background inference must reject stone-like distributions. */
private fun sampleExterior(palette: Palette, grid: RecognizedGrid, coordinate: CellCoordinate, counts: IntArray) {
    for (vertex in 0 until 6) {
        val angle = -PI / 2 + vertex * PI / 3
        for (radius in BACKGROUND_SAMPLE_RADII) {
            val x = (grid.centerX(coordinate) + cos(angle) * grid.spacing * radius).roundToInt()
            val y = (grid.centerY(coordinate) + sin(angle) * grid.spacing * radius).roundToInt()
            if (x in 0 until palette.colorIndices.width && y in 0 until palette.colorIndices.height) {
                counts[palette.colorIndices.get(x, y)]++
            }
        }
    }
}
