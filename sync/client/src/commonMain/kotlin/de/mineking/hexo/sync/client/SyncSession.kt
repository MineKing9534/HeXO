package de.mineking.hexo.sync.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.sync.common.SessionSyncCellHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncData
import de.mineking.hexo.sync.common.SessionSyncLineHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncUpdateRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.coroutines.flow.StateFlow

class SyncSession(
    val data: StateFlow<SessionSyncData>,
    private val wsSession: DefaultClientWebSocketSession,
) {
    suspend fun highlightCell(coordinate: CellCoordinate, highlight: CellHighlight?) {
        wsSession.sendSerialized(SessionSyncCellHighlightRequest(coordinate, highlight))
    }

    suspend fun addLine(line: LineHighlight) {
        wsSession.sendSerialized(SessionSyncLineHighlightRequest(line, remove = false))
    }

    suspend fun removeLine(line: LineHighlight) {
        wsSession.sendSerialized(SessionSyncLineHighlightRequest(line, remove = true))
    }

    suspend fun update(celHighlights: Map<CellCoordinate, CellHighlight>, lineHighlights: List<LineHighlight>) {
        wsSession.sendSerialized(SessionSyncUpdateRequest(celHighlights, lineHighlights))
    }
}

suspend fun SyncSession.reset() = update(emptyMap(), emptyList())

fun SyncSession.asBoard(): Board = MutableBoard().apply {
    lineHighlights += data.value.lineHighlights
    data.value.cellHighlights.forEach { (coordinate, highlight) ->
        this[coordinate].highlight = highlight
    }
}
