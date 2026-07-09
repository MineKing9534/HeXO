package de.mineking.hexo.sync.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.sync.common.SessionSyncCellHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncData
import de.mineking.hexo.sync.common.SessionSyncLineHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncNavigateRequest
import de.mineking.hexo.sync.common.SessionSyncRequest
import de.mineking.hexo.sync.common.SessionSyncUpdateRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.flow.StateFlow

class SyncSession(
    val data: StateFlow<SessionSyncData>,
    private val wsSession: DefaultClientWebSocketSession,
) {
    private suspend fun request(request: SessionSyncRequest) {
        wsSession.sendSerialized(request)
    }

    suspend fun highlightCell(coordinate: CellCoordinate, highlight: CellHighlight?) {
        request(SessionSyncCellHighlightRequest(coordinate, highlight))
    }

    suspend fun addLine(line: LineHighlight) {
        request(SessionSyncLineHighlightRequest(line, remove = false))
    }

    suspend fun removeLine(line: LineHighlight) {
        request(SessionSyncLineHighlightRequest(line, remove = true))
    }

    suspend fun update(celHighlights: Map<CellCoordinate, CellHighlight>, lineHighlights: List<LineHighlight>) {
        request(SessionSyncUpdateRequest(celHighlights, lineHighlights))
    }

    suspend fun navigate(sessionId: SessionId?) {
        request(SessionSyncNavigateRequest(sessionId))
    }

    suspend fun close() {
        wsSession.close()
    }
}

suspend fun SyncSession.reset() = update(emptyMap(), emptyList())

fun SessionSyncData.asBoard(): Board = MutableBoard().apply {
    this.lineHighlights += this@asBoard.lineHighlights
    this@asBoard.cellHighlights.forEach { (coordinate, highlight) ->
        this[coordinate].highlight = highlight
    }
}
