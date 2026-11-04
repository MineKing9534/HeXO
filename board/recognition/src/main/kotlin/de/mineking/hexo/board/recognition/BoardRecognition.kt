package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.recognition.internal.buildPalette
import de.mineking.hexo.board.recognition.internal.classifyStones
import de.mineking.hexo.board.recognition.internal.findCellCandidates
import de.mineking.hexo.board.recognition.internal.fitGrid
import de.mineking.hexo.board.recognition.internal.identifyStones
import de.mineking.hexo.board.recognition.internal.prepareImage
import de.mineking.hexo.board.recognition.internal.sampleCells
import de.mineking.hexo.board.recognition.internal.segmentImage
import java.awt.image.BufferedImage

data class BoardRecognitionOptions(
    val maxImageDimension: Int = 1200,
    val minimumCellSize: Int = 8,
    val colorTolerance: Double = 12.0,
    val gridTolerance: Double = 0.10,
) {
    init {
        require(maxImageDimension in 64..4096)
        require(minimumCellSize in 4..maxImageDimension)
        require(colorTolerance.isFinite() && colorTolerance in 1.0..30.0)
        require(gridTolerance.isFinite() && gridTolerance in 0.02..0.2)
    }
}

enum class RecognitionWarning {
    SingleStoneColor,
    SimilarBrightness,
    UnclassifiedCells,
}

data class BoardRecognitionResult(
    val board: Board,
    val grid: RecognizedGrid,
    val confidence: Double,
    val warnings: Set<RecognitionWarning>,
)

class BoardRecognitionException(message: String) : IllegalArgumentException(message)

fun recognizeBoard(image: BufferedImage, options: BoardRecognitionOptions = BoardRecognitionOptions()): BoardRecognitionResult {
    val prepared = prepareImage(image, options.maxImageDimension)
    val segmentation = segmentImage(prepared)
    val palette = buildPalette(segmentation, options.colorTolerance)

    val cellCandidates = findCellCandidates(segmentation, options.minimumCellSize)
    val gridFit = fitGrid(cellCandidates, options.gridTolerance)

    val cellSamples = sampleCells(palette, gridFit.grid)
    val stones = identifyStones(cellSamples, palette, options.colorTolerance)
    val classification = classifyStones(stones, options.colorTolerance)

    return BoardRecognitionResult(
        board = classification.board,
        grid = gridFit.grid.inOriginalImage(prepared.scale),
        confidence = (gridFit.confidence * classification.confidence).coerceIn(0.0, 1.0),
        warnings = classification.warnings,
    )
}
