package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.binary.SerializableBoard
import kotlinx.io.Source
import kotlinx.serialization.Serializable

@Serializable
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

    companion object {
        val Default = BoardRecognitionOptions(
            maxImageDimension = 1200,
            minimumCellSize = 8,
            colorTolerance = 12.0,
            gridTolerance = 0.10,
        )
    }
}

@Serializable
enum class RecognitionWarning {
    SingleStoneColor,
    SimilarBrightness,
    UnclassifiedCells,
}

@Serializable
data class BoardRecognitionResult(
    val board: SerializableBoard,
    val grid: RecognizedGrid,
    val confidence: Double,
    val warnings: Set<RecognitionWarning>,
)

open class BoardRecognitionException(message: String) : IllegalArgumentException(message)

class BoardRecognitionImageTooLargeException : BoardRecognitionException("Image too large")

interface BoardRecognizer {
    suspend fun recognizeBoard(
        image: Source,
        options: BoardRecognitionOptions = BoardRecognitionOptions.Default,
    ): BoardRecognitionResult
}
