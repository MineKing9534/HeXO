package de.mineking.hexo.board.recognition.internal

private const val MINIMUM_BACKGROUND_SUPPORT = 0.35
private const val MINIMUM_BACKGROUND_ENRICHMENT = 0.20
private const val MINIMUM_BACKGROUND_SUPPORT_GAP = 0.15

private const val MINIMUM_PARTIAL_SAMPLES = 3
private const val MINIMUM_PARTIAL_SUPPORT = 0.80
private const val MINIMUM_PARTIAL_EXTERIOR_SUPPORT = 0.50

private data class BackgroundCandidate(
    val color: LabColor,
    val exterior: Double,
    val interior: Double,
    val partial: Double,
) {
    val score = if (partial >= MINIMUM_PARTIAL_SUPPORT) maxOf(exterior, partial) else exterior

    // Repeated clipped interiors need corroboration from exposed-surface samples.
    val hasPartialCellSupport = partial >= MINIMUM_PARTIAL_SUPPORT && exterior >= MINIMUM_PARTIAL_EXTERIOR_SUPPORT

    // Filled hexes may reach the vertex probes, so exterior color alone is insufficient.
    val hasExteriorSupport = exterior >= MINIMUM_BACKGROUND_SUPPORT &&
        exterior - interior > (1.0 - interior) * MINIMUM_BACKGROUND_ENRICHMENT
}

/** Background must occur in cell interiors as well as the exposed surface near hex vertices. */
internal fun inferEmptyCellColor(sampling: CellSampling, palette: Palette, tolerance: Double): LabColor? {
    val exteriorPixels = sampling.exteriorColorCounts.sum()
    if (exteriorPixels == 0) return null
    val interiorPixels = sampling.samples.sumOf { it.pixelCount }
    if (interiorPixels == 0) return null
    val interiorCounts = IntArray(palette.colors.size)
    for (sample in sampling.samples) {
        for (index in interiorCounts.indices) interiorCounts[index] += sample.colorCounts[index]
    }
    val partialSamples = sampling.samples.filter { !it.isComplete }
    val colors = sampling.samples.map { palette.colors[it.dominantColorIndex] }.distinct()
    val candidates = colors.map { candidate ->
        val matchingIndices = palette.colors.indices.filter { palette.colors[it].distanceTo(candidate) <= tolerance }
        BackgroundCandidate(
            color = candidate,
            exterior = matchingIndices.sumOf { sampling.exteriorColorCounts[it] }.toDouble() / exteriorPixels,
            interior = matchingIndices.sumOf { interiorCounts[it] }.toDouble() / interiorPixels,
            partial = if (partialSamples.size >= MINIMUM_PARTIAL_SAMPLES) {
                partialSamples.count { it.dominantColorIndex in matchingIndices }.toDouble() / partialSamples.size
            } else {
                0.0
            },
        )
    }
    val eligible = candidates.filter { it.hasPartialCellSupport || it.hasExteriorSupport }
    val best = eligible.maxByOrNull { it.score } ?: return null
    val competingSupport = eligible.filter { it.color.distanceTo(best.color) > tolerance }
        .maxOfOrNull { it.score } ?: 0.0
    return best.color.takeIf { best.score - competingSupport >= MINIMUM_BACKGROUND_SUPPORT_GAP }
}
