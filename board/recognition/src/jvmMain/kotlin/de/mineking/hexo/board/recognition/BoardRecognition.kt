package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.recognition.internal.buildPalette
import de.mineking.hexo.board.recognition.internal.classifyStones
import de.mineking.hexo.board.recognition.internal.findCellCandidates
import de.mineking.hexo.board.recognition.internal.fitGrid
import de.mineking.hexo.board.recognition.internal.identifyStones
import de.mineking.hexo.board.recognition.internal.prepareImage
import de.mineking.hexo.board.recognition.internal.sampleCells
import de.mineking.hexo.board.recognition.internal.segmentImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.asOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun BufferedImage.recognizeBoard(options: BoardRecognitionOptions = BoardRecognitionOptions.Default): BoardRecognitionResult {
    val prepared = prepareImage(this, options.maxImageDimension)
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

suspend fun BoardRecognizer.recognizeBoard(
    image: BufferedImage,
    options: BoardRecognitionOptions = BoardRecognitionOptions.Default,
): BoardRecognitionResult {
    val data = Buffer()
    withContext(Dispatchers.IO) {
        ImageIO.write(image, "png", data.asOutputStream())
    }
    return recognizeBoard(data, options)
}
