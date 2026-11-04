package de.mineking.hexo.board.recognition.internal

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.RecognitionWarning
import kotlin.math.abs

// Ignore sparse contrasting pixels left by text or decoration, rather than treating each as a stone.
private const val MINIMUM_STONE_COLOR_COVERAGE = 0.16
private const val SIMILAR_LIGHTNESS_THRESHOLD = 2.0
private const val SINGLE_COLOR_CONFIDENCE_FACTOR = 0.75
private const val PLAYER_COLOR_COUNT = 2

internal data class Stone(val coordinate: CellCoordinate, val color: LabColor)
internal data class StoneClassification(val board: Board, val confidence: Double, val warnings: Set<RecognitionWarning>)

private class StoneGroup(val stones: List<Stone>) {
    val size = stones.size
    val brightness = stones.sumOf { it.color.lightness } / size
}

internal fun identifyStones(samples: List<CellSample>, palette: Palette, colorTolerance: Double): List<Stone> {
    if (samples.isEmpty()) throw BoardRecognitionException("No complete cells are visible")
    val emptyCellColor = inferEmptyCellColor(samples, palette)
    val stoneColorIndices = palette.colors.indices.filter { index ->
        palette.colors[index].distanceTo(emptyCellColor) > colorTolerance
    }

    val stones = mutableListOf<Stone>()
    for (sample in samples) {
        val minimumPixels = sample.pixelCount * MINIMUM_STONE_COLOR_COVERAGE
        val dominantStoneColor = stoneColorIndices
            .filter { sample.colorCounts[it] >= minimumPixels }
            .maxByOrNull { sample.colorCounts[it] }
        if (dominantStoneColor != null) {
            stones += Stone(sample.coordinate, palette.colors[dominantStoneColor])
        }
    }
    return stones
}

private fun inferEmptyCellColor(samples: List<CellSample>, palette: Palette): LabColor {
    // Empty cells are assumed to have the most common dominant color. Lowest palette index wins a tie.
    val cellCounts = IntArray(palette.colors.size)
    for (sample in samples) cellCounts[sample.dominantColorIndex]++
    val backgroundIndex = cellCounts.indices.maxBy { cellCounts[it] }
    return palette.colors[backgroundIndex]
}

/** Keep the two most common appearances and assign owners in descending lightness order. */
internal fun classifyStones(stones: List<Stone>, colorTolerance: Double): StoneClassification {
    if (stones.isEmpty()) return StoneClassification(Board(), 1.0, emptySet())
    val groups = groupStonesByColor(stones, colorTolerance)
    val largestGroups = groups.sortedByDescending { it.size }.take(PLAYER_COLOR_COUNT)
    val ownerGroups = largestGroups.sortedByDescending { it.brightness }
    val warnings = classificationWarnings(groups, ownerGroups)

    val cells = mutableMapOf<CellCoordinate, Cell>()
    for ((groupIndex, group) in ownerGroups.withIndex()) {
        val owner = if (groupIndex == 0) CellOwner.X else CellOwner.O
        for ((coordinate) in group.stones) cells[coordinate] = Cell(owner = owner)
    }
    val retainedFraction = cells.size.toDouble() / stones.size
    val confidence = retainedFraction * if (ownerGroups.size == 1) SINGLE_COLOR_CONFIDENCE_FACTOR else 1.0
    return StoneClassification(Board(cells = cells), confidence, warnings)
}

private fun groupStonesByColor(stones: List<Stone>, tolerance: Double): List<StoneGroup> {
    val clusters = clusterColors(stones.map { it.color }, tolerance)
    val members = mutableMapOf<Int, MutableList<Stone>>()
    for (index in stones.indices) {
        val clusterIndex = clusters.assignments[index]
        members.getOrPut(clusterIndex) { mutableListOf() } += stones[index]
    }
    return members.values.map(::StoneGroup)
}

private fun classificationWarnings(allGroups: List<StoneGroup>, ownerGroups: List<StoneGroup>): Set<RecognitionWarning> {
    val warnings = mutableSetOf<RecognitionWarning>()
    if (ownerGroups.size == 1) warnings += RecognitionWarning.SingleStoneColor
    if (ownerGroups.size == PLAYER_COLOR_COUNT) {
        val brightnessDifference = abs(ownerGroups[0].brightness - ownerGroups[1].brightness)
        if (brightnessDifference < SIMILAR_LIGHTNESS_THRESHOLD) warnings += RecognitionWarning.SimilarBrightness
    }
    if (allGroups.size > PLAYER_COLOR_COUNT) warnings += RecognitionWarning.UnclassifiedCells
    return warnings
}
