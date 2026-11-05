package de.mineking.hexo.board.recognition.internal

import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.HEX_ROW_HEIGHT_RATIO
import de.mineking.hexo.board.recognition.RecognizedGrid
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MINIMUM_GRID_CENTERS = 3
private const val MAXIMUM_ORIGIN_PROPOSALS = 16
private const val MAXIMUM_SPACING_PROPOSALS = 24
private const val MINIMUM_MATCHED_FRACTION = 0.4
private const val MINIMUM_NEIGHBOR_DISTANCE_FACTOR = 0.9
private const val MAXIMUM_NEIGHBOR_DISTANCE_FACTOR = 2.2
private const val SPACING_BINS_PER_PIXEL = 2.0

internal data class GridFit(val grid: RecognizedGrid, val confidence: Double)

private data class GridHypothesis(
    val grid: RecognizedGrid,
    val matchedCenters: List<CellCandidate>,
    val sumSquaredError: Double,
) {
    fun isBetterThan(other: GridHypothesis): Boolean {
        val moreMatches = matchedCenters.size > other.matchedCenters.size
        val sameMatchCount = matchedCenters.size == other.matchedCenters.size
        return moreMatches || (sameMatchCount && sumSquaredError < other.sumSquaredError)
    }
}

/** Vote for likely spacings, compare possible origins, then refine the best-supported grid. */
internal fun fitGrid(centers: List<CellCandidate>, tolerance: Double): GridFit {
    if (centers.size < MINIMUM_GRID_CENTERS) {
        throw BoardRecognitionException("Not enough cell interiors or stones to detect a hex grid")
    }

    val spacings = proposeSpacings(centers, tolerance)
    val originCount = minOf(centers.size, MAXIMUM_ORIGIN_PROPOSALS)
    val origins = List(originCount) { centers[it * centers.size / originCount] }
    var best: GridHypothesis? = null

    for (spacing in spacings) {
        for ((centerX, centerY) in origins) {
            val grid = RecognizedGrid(centerX, centerY, spacing)
            val hypothesis = assessGrid(grid, centers, tolerance)

            if (best == null || hypothesis.isBetterThan(best)) best = hypothesis
        }
    }

    val winner = best ?: throw BoardRecognitionException("Could not find repeated hex-grid spacing")

    val matchCount = winner.matchedCenters.size
    if (matchCount < MINIMUM_GRID_CENTERS || matchCount < centers.size * MINIMUM_MATCHED_FRACTION) {
        throw BoardRecognitionException("Cell centers do not agree on a hex grid")
    }

    return GridFit(refineGrid(winner), matchCount.toDouble() / centers.size)
}

private fun proposeSpacings(centers: List<CellCandidate>, tolerance: Double): List<Double> {
    val medianWidth = centers.map { it.width }.sorted()[centers.size / 2].toDouble()
    val minimumDistance = medianWidth * MINIMUM_NEIGHBOR_DISTANCE_FACTOR
    val maximumDistance = medianWidth * MAXIMUM_NEIGHBOR_DISTANCE_FACTOR

    val votes = mutableMapOf<Int, Int>()
    for (first in centers.indices) {
        for (second in first + 1 until centers.size) {
            val dx = abs(centers[first].centerX - centers[second].centerX)
            val dy = abs(centers[first].centerY - centers[second].centerY)
            val distance = sqrt(dx * dx + dy * dy)
            if (distance !in minimumDistance..maximumDistance) continue

            // Hex neighbors lie on horizontal or +/-60 degree axes; on a diagonal, dx = dy/sqrt(3).
            val horizontal = dy < distance * tolerance
            val diagonal = abs(dx - dy / (2 * HEX_ROW_HEIGHT_RATIO)) < distance * tolerance
            if (horizontal || diagonal) {
                val spacingBin = (distance * SPACING_BINS_PER_PIXEL).roundToInt()
                votes[spacingBin] = (votes[spacingBin] ?: 0) + 1
            }
        }
    }

    return votes.entries.sortedByDescending { it.value }
        .take(MAXIMUM_SPACING_PROPOSALS)
        .map { it.key / SPACING_BINS_PER_PIXEL }
}

private fun assessGrid(grid: RecognizedGrid, centers: List<CellCandidate>, tolerance: Double): GridHypothesis {
    val maximumErrorSquared = grid.spacing * grid.spacing * tolerance * tolerance
    val matches = mutableListOf<CellCandidate>()
    var sumSquaredError = 0.0

    for (center in centers) {
        val coordinate = grid.coordinateAt(center.centerX, center.centerY)
        val dx = grid.centerX(coordinate) - center.centerX
        val dy = grid.centerY(coordinate) - center.centerY
        val squaredError = dx * dx + dy * dy
        if (squaredError <= maximumErrorSquared) {
            matches += center
            sumSquaredError += squaredError
        }
    }

    return GridHypothesis(grid, matches, sumSquaredError)
}

private fun refineGrid(hypothesis: GridHypothesis): RecognizedGrid {
    // Fit pixel = origin + spacing * (q + r/2, sqrt(3)/2 * r) by least squares.
    // Centering both point sets separates the spacing estimate from the origin estimate.
    val grid = hypothesis.grid
    val centers = hypothesis.matchedCenters
    val coordinates = centers.map { grid.coordinateAt(it.centerX, it.centerY) }
    val normalizedX = coordinates.map { it.q + it.r / 2.0 }
    val normalizedY = coordinates.map { it.r * HEX_ROW_HEIGHT_RATIO }
    val meanGridX = normalizedX.sum() / coordinates.size
    val meanGridY = normalizedY.sum() / coordinates.size
    val meanPixelX = centers.sumOf { it.centerX } / centers.size
    val meanPixelY = centers.sumOf { it.centerY } / centers.size

    var covariance = 0.0
    var gridVariance = 0.0

    for (index in coordinates.indices) {
        val gridOffsetX = normalizedX[index] - meanGridX
        val gridOffsetY = normalizedY[index] - meanGridY
        val pixelOffsetX = centers[index].centerX - meanPixelX
        val pixelOffsetY = centers[index].centerY - meanPixelY
        covariance += gridOffsetX * pixelOffsetX + gridOffsetY * pixelOffsetY
        gridVariance += gridOffsetX * gridOffsetX + gridOffsetY * gridOffsetY
    }

    if (gridVariance == 0.0) return grid
    val spacing = covariance / gridVariance
    return RecognizedGrid(meanPixelX - spacing * meanGridX, meanPixelY - spacing * meanGridY, spacing)
}
