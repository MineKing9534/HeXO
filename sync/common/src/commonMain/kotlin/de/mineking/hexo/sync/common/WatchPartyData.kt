package de.mineking.hexo.sync.common

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.hds.session.SessionId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class WatchPartyId(val value: String)

@Serializable
data class WatchPartyData(
    val id: WatchPartyId,
    val sessionId: SessionId?,
    val move: Int,
    val cellHighlights: Map<CellCoordinate, CellHighlight>,
    val lineHighlights: List<LineHighlight>,
)
