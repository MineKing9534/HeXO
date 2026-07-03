package de.mineking.hexo.sync.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.sync.common.WatchPartyCellHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyMoveCountRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateRequest
import de.mineking.hexo.sync.common.WatchPartyRequest
import de.mineking.hexo.sync.common.WatchPartyUpdateRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.flow.StateFlow

class WatchParty internal constructor(
    val data: StateFlow<WatchPartyData>,
    private val wsSession: DefaultClientWebSocketSession,
) {
    private val onClose = mutableListOf<() -> Unit>()

    fun onClose(block: () -> Unit) {
        onClose += block
    }

    private suspend fun request(request: WatchPartyRequest) {
        wsSession.sendSerialized(request)
    }

    suspend fun highlightCell(coordinate: CellCoordinate, highlight: CellHighlight?) {
        request(WatchPartyCellHighlightRequest(coordinate, highlight))
    }

    suspend fun addLine(line: LineHighlight) {
        request(WatchPartyLineHighlightRequest(line, remove = false))
    }

    suspend fun removeLine(line: LineHighlight) {
        request(WatchPartyLineHighlightRequest(line, remove = true))
    }

    suspend fun update(celHighlights: Map<CellCoordinate, CellHighlight>, lineHighlights: List<LineHighlight>) {
        request(WatchPartyUpdateRequest(celHighlights, lineHighlights))
    }

    suspend fun navigate(sessionId: SessionId?) {
        request(WatchPartyNavigateRequest(sessionId))
    }

    suspend fun adjustMoveCount(move: Int) {
        request(WatchPartyMoveCountRequest(move))
    }

    suspend fun close() {
        onClose.forEach { it() }
        wsSession.close()
    }
}

fun WatchPartyData.asBoard(): Board = MutableBoard().apply {
    this.lineHighlights += this@asBoard.lineHighlights
    this@asBoard.cellHighlights.forEach { (coordinate, highlight) ->
        this[coordinate].highlight = highlight
    }
}
