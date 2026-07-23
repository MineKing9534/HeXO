package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.withAlpha
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.web.playerColor
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.worker.AnalysisInput
import de.mineking.hexo.web.worker.AnalysisWorker
import kotlinx.coroutines.awaitCancellation

data class AnalyzerTurn(val player: CellOwner, val remaining: Int)

sealed interface BoardAnalyzerState {
    data object Loading : BoardAnalyzerState
    data class Data(
        val threat: FindWinResult,
        val defense: FindDefenseResult,
    ) : BoardAnalyzerState
}

@Composable
fun BoardAnalyzerState.drawLayer(): RenderingContext.() -> Unit {
    if (this !is BoardAnalyzerState.Data) return {}

    val theme by rememberTheme()
    return { drawAnalyzerOverlay(theme, this@drawLayer) }
}

@Composable
fun rememberBoardAnalysis(board: Board, turn: AnalyzerTurn): BoardAnalyzerState {
    val boardOwners = remember(board) {
        board.cells.mapNotNull { (coordinate, cell) ->
            val owner = cell.owner ?: return@mapNotNull null
            coordinate to owner
        }.sortedWith(compareBy({ it.first.q }, { it.first.r }, { it.second.name }))
    }

    var result by remember(boardOwners, turn) { mutableStateOf<BoardAnalyzerState>(BoardAnalyzerState.Loading) }
    var requestId by remember { mutableStateOf(0) }

    LaunchedEffect(boardOwners, turn) {
        val currentRequestId = ++requestId

        val worker = AnalysisWorker { output ->
            if (output.requestId == requestId) {
                result = BoardAnalyzerState.Data(output.threat, output.defense)
            }
        }

        try {
            worker.postInput(
                AnalysisInput(
                    requestId = currentRequestId,
                    board = board,
                    player = turn.player,
                    remaining = turn.remaining,
                ),
            )
            awaitCancellation()
        } finally {
            worker.terminate()
        }
    }

    return result
}

private val defenseOverlayColor = Color.rgb(0x34d399)
private enum class AnalyzerMarkerStyle {
    Opportunity,
    Threat,
    Defense,
}

private fun RenderingContext.drawAnalyzerOverlay(theme: BaseTheme, result: BoardAnalyzerState.Data) {
    if (result.threat is FindWinResult.Win) {
        drawThreatOverlay(theme, result.threat, AnalyzerMarkerStyle.Opportunity)
    } else if (result.defense is FindDefenseResult.Threat) {
        drawDefenseOverlay(theme, result.defense)
    }
}

private fun RenderingContext.drawThreatOverlay(
    theme: BaseTheme,
    result: FindWinResult.Win,
    markerStyle: AnalyzerMarkerStyle,
    excludedCells: Set<CellCoordinate> = emptySet(),
) {
    result.turns.forEachIndexed { index, (player, cells) ->
        cells.forEach { cell ->
            if (cell in excludedCells) return@forEach

            val point = layout.run { cell.toPixel() }
            val color = theme.playerColor(player)

            drawOverlayTarget(
                point = point,
                color = color,
                backgroundColor = theme.backgroundColor,
                label = "${index + 1}",
                markerStyle = markerStyle,
            )
        }
    }
}

private fun RenderingContext.drawDefenseOverlay(theme: BaseTheme, result: FindDefenseResult.Threat) {
    val defense = result.defenses.firstOrNull()
    val defenseCells = defense?.toSet().orEmpty()

    drawThreatOverlay(
        theme = theme,
        result = result.threat,
        markerStyle = AnalyzerMarkerStyle.Threat,
        excludedCells = defenseCells,
    )

    defense?.forEach { cell ->
        val point = layout.run { cell.toPixel() }
        drawOverlayTarget(
            point = point,
            color = defenseOverlayColor,
            backgroundColor = theme.backgroundColor,
            label = "+",
            markerStyle = AnalyzerMarkerStyle.Defense,
        )
    }
}

private fun RenderingContext.drawOverlayTarget(
    point: Point,
    color: Color,
    backgroundColor: Color,
    label: String,
    markerStyle: AnalyzerMarkerStyle,
) {
    val target = point.createHex(hexSize * 0.84)
    backend.drawPolygon(
        shape = target,
        color = color.withAlpha(if (markerStyle == AnalyzerMarkerStyle.Threat) 24 else 38),
        outline = Stroke(color.withAlpha(210), 3.0.relativeWidth()),
        borderRadius = 2.5.relativeWidth(),
    )

    if (markerStyle == AnalyzerMarkerStyle.Threat) {
        backend.drawPolygon(
            shape = point.createHex(hexSize * 0.38),
            color = color.withAlpha(230),
            outline = Stroke(backgroundColor.withAlpha(210), 3.0.relativeWidth()),
            borderRadius = 2.0.relativeWidth(),
        )
    } else {
        backend.drawLine(
            from = point,
            to = point,
            stroke = Stroke(backgroundColor.withAlpha(230), (hexSize * 0.78).toFloat()),
            outline = Stroke(color, 4.0.relativeWidth()),
        )
    }

    backend.drawString(
        point = point,
        text = label,
        maxWidth = hexSize * 0.35,
        fontSize = (hexSize * 0.5).toFloat(),
        font = FontType.SansSerifBold,
        color = if (markerStyle == AnalyzerMarkerStyle.Threat) backgroundColor else color,
    )
}
