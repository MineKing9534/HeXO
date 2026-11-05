package de.mineking.hexo.board.recognition.internal

import boofcv.alg.color.ColorLab
import boofcv.alg.filter.misc.AverageDownSampleOps
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayF32
import boofcv.struct.image.GrayU8
import boofcv.struct.image.Planar
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

internal data class PreparedImage(
    val lab: Planar<GrayF32>,
    val scale: Double,
)

internal fun prepareImage(image: BufferedImage, maximumDimension: Int): PreparedImage {
    val scale = minOf(1.0, maximumDimension.toDouble() / maxOf(image.width, image.height))
    val width = (image.width * scale).roundToInt().coerceAtLeast(1)
    val height = (image.height * scale).roundToInt().coerceAtLeast(1)

    val fullSizeRgb = image.toOpaqueRgb()
    val resizedRgb = fullSizeRgb.createNew(width, height)

    AverageDownSampleOps.down(fullSizeRgb, resizedRgb)

    val lab = Planar(GrayF32::class.java, width, height, 3)
    ColorLab.rgbToLab(resizedRgb, lab)
    return PreparedImage(lab, scale)
}

private fun BufferedImage.toOpaqueRgb(): Planar<GrayU8> {
    // Normalize storage formats and composite alpha before passing three RGB channels to BoofCV.
    val opaque = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = opaque.createGraphics()
    try {
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(this, 0, 0, null)
    } finally {
        graphics.dispose()
    }

    return ConvertBufferedImage.convertFromPlanar(opaque, null, true, GrayU8::class.java)
}
