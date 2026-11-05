package de.mineking.hexo.board.recognition.internal

private const val MINIMUM_CELL_ASPECT_RATIO = 0.65
private const val MAXIMUM_CELL_ASPECT_RATIO = 1.4
private const val MINIMUM_INTERIOR_FILL = 0.52
private const val BACKGROUND_AREA_DIVISOR = 3
private const val CENTER_MERGE_RADIUS_SQUARED_FRACTION = 0.09
private const val MAX_CELL_CANDIDATES = 800

internal data class CellCandidate(val centerX: Double, val centerY: Double, val width: Int, val height: Int, val area: Int)

internal fun findCellCandidates(segmentation: ImageSegmentation, minimumSize: Int): List<CellCandidate> {
    val width = segmentation.regionLabels.width
    val height = segmentation.regionLabels.height

    val candidates = segmentation.regions
        .filter { it.isCellInterior(width, height, minimumSize) }
        .map { CellCandidate((it.minX + it.maxX) / 2.0, (it.minY + it.maxY) / 2.0, it.width, it.height, it.pixelCount) }

    // Prefer the enclosing stone to a smaller decoration at the same center.
    val distinctCenters = mutableListOf<CellCandidate>()
    for (candidate in candidates.sortedByDescending { it.area }) {
        if (distinctCenters.none { it.sharesCenterWith(candidate) }) {
            distinctCenters += candidate
        }
    }

    // Even spatial sampling keeps fitting bounded without favoring a corner of a dense board.
    val ordered = distinctCenters.sortedWith(compareBy({ it.centerY }, { it.centerX }))
    if (ordered.size <= MAX_CELL_CANDIDATES) return ordered
    return List(MAX_CELL_CANDIDATES) { ordered[it * ordered.size / MAX_CELL_CANDIDATES] }
}

private fun ImageRegion.isCellInterior(imageWidth: Int, imageHeight: Int, minimumSize: Int): Boolean {
    val fullyVisible = minX > 0 && minY > 0 && maxX < imageWidth - 1 && maxY < imageHeight - 1
    val largeEnough = minOf(width, height) >= minimumSize
    val aspectRatio = width.toDouble() / height
    val fillFraction = pixelCount.toDouble() / (width * height)
    val compactShape = aspectRatio in MINIMUM_CELL_ASPECT_RATIO..MAXIMUM_CELL_ASPECT_RATIO && fillFraction >= MINIMUM_INTERIOR_FILL
    val smallerThanBackground = pixelCount < imageWidth * imageHeight / BACKGROUND_AREA_DIVISOR

    // Text and outlines are small or sparse; cell interiors remain compact even with holes from labels.
    return fullyVisible && largeEnough && compactShape && smallerThanBackground
}

private fun CellCandidate.sharesCenterWith(other: CellCandidate): Boolean {
    val dx = centerX - other.centerX
    val dy = centerY - other.centerY

    val shortestSide = minOf(width, height)
    val mergeRadiusSquared = shortestSide * shortestSide * CENTER_MERGE_RADIUS_SQUARED_FRACTION

    return dx * dx + dy * dy < mergeRadiusSquared
}
