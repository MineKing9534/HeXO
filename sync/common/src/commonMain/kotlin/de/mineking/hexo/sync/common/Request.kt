package de.mineking.hexo.sync.common

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import kotlinx.serialization.Serializable

@Serializable
sealed interface SessionSyncRequest

@Serializable
data class SessionSyncCellHighlightRequest(
    val coordinate: CellCoordinate,
    val highlight: CellHighlight?,
) : SessionSyncRequest

@Serializable
data class SessionSyncLineHighlightRequest(
    val line: LineHighlight,
) : SessionSyncRequest
