package de.mineking.hexo.board.recognition.internal

import boofcv.struct.image.GrayS32

internal data class Palette(
    val colors: List<LabColor>,
    /** Each pixel contains an index into colors, so disconnected regions can share a palette entry. */
    val colorIndices: GrayS32,
)

internal fun buildPalette(segmentation: ImageSegmentation, colorTolerance: Double): Palette {
    // Keep the palette finer than the final player-color groups, preserving shades for cell sampling.
    val clusters = clusterColors(segmentation.regions.map { it.meanColor }, colorTolerance / 2)
    val regionLabels = segmentation.regionLabels
    val colorIndices = GrayS32(regionLabels.width, regionLabels.height)

    for (pixel in colorIndices.data.indices) {
        val regionIndex = regionLabels.data[pixel]
        colorIndices.data[pixel] = clusters.assignments[regionIndex]
    }

    return Palette(clusters.centers, colorIndices)
}
