package de.mineking.hexo.watchparty.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.watchparty.common.WatchPartyCellRequest
import de.mineking.hexo.watchparty.common.WatchPartyClearHighlightsRequest
import de.mineking.hexo.watchparty.common.WatchPartyData
import de.mineking.hexo.watchparty.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.watchparty.common.WatchPartyMoveCountRequest
import de.mineking.hexo.watchparty.common.WatchPartyNavigateRequest
import de.mineking.hexo.watchparty.common.WatchPartyNavigateTarget
import de.mineking.hexo.watchparty.common.WatchPartyRequest
import de.mineking.hexo.watchparty.common.WatchPartyUpdateRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class WatchPartyCloseReason(val closedByServer: Boolean)

class WatchParty internal constructor(
    private var wsSession: DefaultClientWebSocketSession,
    data: WatchPartyData,
) {
    private val onClose = mutableListOf<(WatchPartyCloseReason) -> Unit>()
    private var closed = false

    val connected: StateFlow<Boolean>
        field = MutableStateFlow(true)

    val data: StateFlow<WatchPartyData>
        field = MutableStateFlow(data)

    internal val isClosed get() = closed

    val id get() = data.value.id

    internal fun onClosed(reason: WatchPartyCloseReason) {
        closed = true
        connected.value = false
        onClose.forEach { it(reason) }
        onClose.clear()
    }

    internal fun onDisconnected() {
        connected.value = false
    }

    internal fun onReconnected(session: DefaultClientWebSocketSession, initial: WatchPartyData) {
        wsSession = session
        data.value = initial
        connected.value = true
    }

    internal fun onData(data: WatchPartyData) {
        this.data.value = data
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

    suspend fun clearHighlights() {
        request(WatchPartyClearHighlightsRequest)
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

    fun onClose(block: (WatchPartyCloseReason) -> Unit) {
        onClose += block
    }

    suspend fun close() {
        closed = true
        connected.value = false
        onClosed(WatchPartyCloseReason(closedByServer = false))
        wsSession.close()
    }
}
