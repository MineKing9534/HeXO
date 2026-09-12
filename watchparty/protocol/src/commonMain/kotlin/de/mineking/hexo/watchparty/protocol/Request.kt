package de.mineking.hexo.watchparty.protocol

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.model.WatchPartyNavigateTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchPartyConnectData(
    val watchPartyId: WatchPartyId,
    val connectionId: WatchPartyConnectionId,
    val detachOnClose: Boolean,
)

@Serializable
sealed interface WatchPartyRequest

@Serializable
sealed interface WatchPartyTransactionChildRequest : WatchPartyRequest

@Serializable
@SerialName("navigate")
data class WatchPartyNavigateRequest(
    val target: WatchPartyNavigateTarget?,
) : WatchPartyRequest

@Serializable
@SerialName("move")
data class WatchPartyMoveCountRequest(
    val move: Int,
) : WatchPartyRequest

@Serializable
@SerialName("update")
data class WatchPartyUpdateRequest(
    val board: Board,
) : WatchPartyTransactionChildRequest

@Serializable
@SerialName("cell")
data class WatchPartyCellRequest(
    val coordinate: CellCoordinate,
    val cell: CellOverride,
) : WatchPartyTransactionChildRequest

@Serializable
@SerialName("line")
data class WatchPartyLineHighlightRequest(
    val line: LineHighlight,
    val remove: Boolean,
) : WatchPartyTransactionChildRequest

@Serializable
@SerialName("clear-highlights")
data object WatchPartyClearHighlightsRequest : WatchPartyTransactionChildRequest

@Serializable
@SerialName("undo")
data object WatchPartyUndoRequest : WatchPartyTransactionChildRequest

@Serializable
@SerialName("redo")
data object WatchPartyRedoRequest : WatchPartyTransactionChildRequest

@Serializable
@SerialName("transaction")
data class WatchPartyTransactionRequest(
    val actions: List<WatchPartyTransactionChildRequest>,
) : WatchPartyRequest
