package de.mineking.hexo.watchparty.protocol

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.watchparty.model.WatchPartyNavigateTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
sealed interface WatchPartyRequest

@Serializable
@SerialName("ping")
data object WatchPartyPingRequest : WatchPartyRequest

@Serializable
@SerialName("action")
data class WatchPartyActionRequest(
    val id: WatchPartyRequestId,
    val generation: Long?,
    val action: WatchPartyAction,
) : WatchPartyRequest

@Serializable
sealed interface WatchPartyAction

@Serializable
sealed interface WatchPartyTransactionChildAction : WatchPartyAction

@Serializable
@SerialName("navigate")
data class WatchPartyNavigateRequest(
    val target: WatchPartyNavigateTarget?,
) : WatchPartyAction

@Serializable
@SerialName("move")
data class WatchPartyMoveCountRequest(
    val move: Int,
) : WatchPartyAction

@Serializable
@SerialName("update")
data class WatchPartyUpdateRequest(
    val board: Board,
) : WatchPartyTransactionChildAction

@Serializable
@SerialName("cell")
data class WatchPartyCellRequest(
    val coordinate: CellCoordinate,
    val cell: CellOverride,
) : WatchPartyTransactionChildAction

@Serializable
@SerialName("line")
data class WatchPartyLineHighlightRequest(
    val line: LineHighlight,
    val remove: Boolean,
) : WatchPartyTransactionChildAction

@Serializable
@SerialName("clear-highlights")
data object WatchPartyClearHighlightsRequest : WatchPartyTransactionChildAction

@Serializable
@SerialName("undo")
data object WatchPartyUndoRequest : WatchPartyTransactionChildAction

@Serializable
@SerialName("redo")
data object WatchPartyRedoRequest : WatchPartyTransactionChildAction

@Serializable
@SerialName("transaction")
data class WatchPartyTransactionRequest(
    val actions: List<WatchPartyTransactionChildAction>,
) : WatchPartyAction

@JvmInline
@Serializable
value class WatchPartyRequestId(val value: String)
