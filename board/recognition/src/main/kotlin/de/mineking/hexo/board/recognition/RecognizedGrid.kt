package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.CellCoordinate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

// In a pointy-top hex grid, adjacent rows are sqrt(3)/2 times the horizontal center spacing apart.
internal const val HEX_ROW_HEIGHT_RATIO = 0.8660254037844386

/** Pixel geometry in the original image; spacing is the horizontal distance between adjacent centers. */
data class RecognizedGrid(val originX: Double, val originY: Double, val spacing: Double) {
    fun centerX(coordinate: CellCoordinate) = originX + spacing * (coordinate.q + coordinate.r / 2.0)
    fun centerY(coordinate: CellCoordinate) = originY + spacing * HEX_ROW_HEIGHT_RATIO * coordinate.r

    /** Invert the pixel projection, then round to the nearest hex using cube coordinates q + r + s = 0. */
    internal fun coordinateAt(x: Double, y: Double): CellCoordinate {
        val fractionalR = (y - originY) / (spacing * HEX_ROW_HEIGHT_RATIO)
        val fractionalQ = (x - originX) / spacing - fractionalR / 2
        var roundedQ = fractionalQ.roundToInt()
        var roundedR = fractionalR.roundToInt()
        val roundedS = (-fractionalQ - fractionalR).roundToInt()
        val qError = abs(fractionalQ - roundedQ)
        val rError = abs(fractionalR - roundedR)
        val sError = abs(-fractionalQ - fractionalR - roundedS)

        // Correct the coordinate with the largest rounding error to restore the cube constraint.
        if (qError > rError && qError > sError) {
            roundedQ = -roundedR - roundedS
        } else if (rError > sError) {
            roundedR = -roundedQ - roundedS
        }
        return CellCoordinate(roundedQ, roundedR)
    }

    /** Enumerate only centers whose entire sampling square fits inside the image. */
    internal fun completeCellCoordinates(width: Int, height: Int, margin: Int): Sequence<CellCoordinate> = sequence {
        val minimumRow = ceil((margin - originY) / (spacing * HEX_ROW_HEIGHT_RATIO)).toInt()
        val maximumRow = floor((height - 1 - margin - originY) / (spacing * HEX_ROW_HEIGHT_RATIO)).toInt()
        for (r in minimumRow..maximumRow) {
            val minimumColumn = ceil((margin - originX) / spacing - r / 2.0).toInt()
            val maximumColumn = floor((width - 1 - margin - originX) / spacing - r / 2.0).toInt()
            for (q in minimumColumn..maximumColumn) yield(CellCoordinate(q, r))
        }
    }

    internal fun inOriginalImage(processingScale: Double) = RecognizedGrid(
        originX = originX / processingScale,
        originY = originY / processingScale,
        spacing = spacing / processingScale,
    )
}
