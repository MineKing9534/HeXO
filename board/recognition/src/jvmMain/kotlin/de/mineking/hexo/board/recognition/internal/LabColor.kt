package de.mineking.hexo.board.recognition.internal

import boofcv.alg.color.ColorDifference

internal data class LabColor(val lightness: Double, val a: Double, val b: Double) {
    fun distanceTo(other: LabColor): Double = ColorDifference.deltaECIEDE2000(
        lightness, a, b,
        other.lightness, other.a, other.b,
    )
}
