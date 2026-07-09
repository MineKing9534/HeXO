package de.mineking.hexo.sync.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.sync.common.WatchPartyCellRequest
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyMoveCountRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateTarget
import de.mineking.hexo.sync.common.WatchPartyRequest
import de.mineking.hexo.sync.common.WatchPartyUpdateRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.flow.StateFlow

data class WatchPartyCloseReason(val closedByServer: Boolean)

class WatchParty internal constructor(
    val data: StateFlow<WatchPartyData>,
    private val wsSession: DefaultClientWebSocketSession,
) {
    private val onClose = mutableListOf<(WatchPartyCloseReason) -> Unit>()

    fun onClose(block: (WatchPartyCloseReason) -> Unit) {
        onClose += block
    }

    private suspend fun request(request: WatchPartyRequest) {
        wsSession.sendSerialized(request)
    }

    suspend fun updateCell(coordinate: CellCoordinate, cell: CellOverride) {
        request(WatchPartyCellRequest(coordinate, cell))
    }

    suspend fun addLine(line: LineHighlight) {
        request(WatchPartyLineHighlightRequest(line, remove = false))
    }

    suspend fun removeLine(line: LineHighlight) {
        request(WatchPartyLineHighlightRequest(line, remove = true))
    }

    suspend fun update(board: Board) {
        request(WatchPartyUpdateRequest(board))
    }

    suspend fun navigate(target: WatchPartyNavigateTarget?) {
        request(WatchPartyNavigateRequest(target))
    }

    suspend fun adjustMoveCount(move: Int) {
        request(WatchPartyMoveCountRequest(move))
    }

    internal fun onClose(reason: WatchPartyCloseReason) {
        onClose.forEach { it(reason) }
        onClose.clear()
    }

    suspend fun close() {
        onClose(WatchPartyCloseReason(closedByServer = false))
        wsSession.close()
    }
}
