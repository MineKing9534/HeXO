package de.mineking.hexo.launcher

import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.RemoteBoardParser
import de.mineking.hexo.board.parse.caching
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.parse.or
import de.mineking.hexo.board.render.caching
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.ErrorMessage
import de.mineking.hexo.board.render.image.ImageSizeLimitExceededException
import de.mineking.hexo.board.render.image.drawExceptionMessages
import de.mineking.hexo.board.render.image.limitSize
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.board.render.limitConcurrency
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.entries
import de.mineking.hexo.utils.cache.megabytes
import kotlin.math.roundToLong

fun createBoardParser(hds: HdsApiClient) = (RemoteBoardParser(hds) or BoardParser.Default)
    .focusWinningRows()
    .caching(CacheConfiguration(
        sizeLimit = 16.entries,
        expiration = null,
        evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    ))

fun createBoardRenderer(errorHandler: suspend (ImageSizeLimitExceededException) -> ErrorMessage) = BufferedImageBoardRenderer.Default
    .limitSize(64.megabytes)
    .drawExceptionMessages { error ->
        when (error) {
            is ImageSizeLimitExceededException -> errorHandler(error)
            else -> null
        }
    }
    .limitConcurrency(10)
    .outputPngBytes()
    .caching(CacheConfiguration(
        sizeLimit = 128.megabytes,
        expiration = null,
        evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    ))

fun Long.formatBytes(): String {
    val units = arrayOf("B", "KiB", "MiB", "GiB")
    var value = toDouble()
    var unit = 0

    while (kotlin.math.abs(value) >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }

    if (unit == 0) return "$this ${units[unit]}"

    val rounded = (value * 10).roundToLong() / 10.0
    val formatted = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return "$formatted ${units[unit]}"
}
