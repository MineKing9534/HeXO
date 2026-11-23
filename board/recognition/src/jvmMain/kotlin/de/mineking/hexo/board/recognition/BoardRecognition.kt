package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.recognition.internal.buildPalette
import de.mineking.hexo.board.recognition.internal.classifyStones
import de.mineking.hexo.board.recognition.internal.findCellCandidates
import de.mineking.hexo.board.recognition.internal.fitGrid
import de.mineking.hexo.board.recognition.internal.identifyStones
import de.mineking.hexo.board.recognition.internal.inferEmptyCellColor
import de.mineking.hexo.board.recognition.internal.prepareImage
import de.mineking.hexo.board.recognition.internal.sampleCells
import de.mineking.hexo.board.recognition.internal.segmentImage
import de.mineking.hexo.utils.types.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.asOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun BufferedImage.recognizeBoard(
    options: BoardRecognitionOptions = BoardRecognitionOptions.Default,
): Result<BoardRecognitionResult, BoardRecognitionError> {
    val prepared = prepareImage(this, options.maxImageDimension)
    val segmentation = segmentImage(prepared)
    val palette = buildPalette(segmentation, options.colorTolerance)

    val cellCandidates = findCellCandidates(segmentation, options.minimumCellSize)
    val gridFit = fitGrid(cellCandidates, options.gridTolerance)
        ?: return Result.Error(GridNotFoundError)

    val grid = gridFit.grid.inOriginalImage(prepared.scale)
    val cellSamples = sampleCells(palette, gridFit.grid)
    val completeSamples = cellSamples.samples.filter { it.isComplete }
    if (completeSamples.isEmpty()) {
        return Result.Error(NoCompleteCellsError)
    }
    val emptyCellColor = inferEmptyCellColor(cellSamples, palette, options.colorTolerance)
        ?: return Result.Error(AmbiguousBackgroundError)
    val stones = identifyStones(completeSamples, palette, options.colorTolerance, emptyCellColor)
    if (stones.isEmpty()) {
        return Result.Error(NoStonesFoundError)
    }
    val classification = classifyStones(stones, options.colorTolerance)

    return Result.Success(
        BoardRecognitionResult(
            board = classification.board,
            grid = grid,
            confidence = (gridFit.confidence * classification.confidence).coerceIn(0.0, 1.0),
            warnings = classification.warnings,
        ),
    )
}

suspend fun BoardRecognizer.recognizeBoard(
    image: BufferedImage,
    options: BoardRecognitionOptions = BoardRecognitionOptions.Default,
): Result<BoardRecognitionResult, BoardRecognitionError> {
    val data = Buffer()
    withContext(Dispatchers.IO) {
        ImageIO.write(image, "png", data.asOutputStream())
    }
    return recognizeBoard(data, options)
}
