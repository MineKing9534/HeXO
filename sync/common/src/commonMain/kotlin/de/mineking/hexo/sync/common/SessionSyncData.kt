package de.mineking.hexo.sync.common

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class SessionSyncId(val value: String)

@Serializable
data class SessionSyncData(
    val id: SessionSyncId,
    val cellHighlights: Map<CellCoordinate, CellHighlight>,
    val lineHighlights: List<LineHighlight>,
)
