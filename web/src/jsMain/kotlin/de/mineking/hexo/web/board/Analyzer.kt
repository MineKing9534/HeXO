package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.CanvasRenderingBackend
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.css
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.withAlpha
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.web.icons.SHIELD_ICON_PATH
import de.mineking.hexo.web.playerColor
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.worker.AnalysisInput
import de.mineking.hexo.web.worker.AnalysisWorker
import kotlinx.coroutines.awaitCancellation
import org.w3c.dom.Path2D

data class AnalyzerTurn(val player: CellOwner, val remaining: Int)

sealed interface BoardAnalyzerState {
    data object Loading : BoardAnalyzerState
    data class Data(
        val threat: FindWinResult,
        val defense: FindDefenseResult,
    ) : BoardAnalyzerState
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

@Composable
fun BoardAnalyzerState.renderingHook(): BoardRenderingHook {
    val theme by rememberTheme()
    return renderingHook(theme)
}

fun BoardAnalyzerState.renderingHook(theme: BaseTheme) = object : BoardRenderingHook {
    override fun RenderingContext.drawMiddleLayer() {
        if (this@renderingHook !is BoardAnalyzerState.Data) return

        if (threat is FindWinResult.Win) {
            drawFirstThreatMoveHighlight(theme, threat)
        } else if (defense is FindDefenseResult.Threat) {
            drawFirstThreatMoveHighlight(theme, defense.threat)
        }
    }

    override fun RenderingContext.drawTopLayer() {
        if (this@renderingHook !is BoardAnalyzerState.Data) return
        drawAnalyzerOverlay(theme, this@renderingHook)
    }
}

private val defenseOverlayColor = Color.rgb(0x34d399)

private fun RenderingContext.drawAnalyzerOverlay(theme: BaseTheme, state: BoardAnalyzerState.Data) {
    if (state.threat is FindWinResult.Win) {
        drawThreatOverlay(theme, state.threat)
    } else if (state.defense is FindDefenseResult.Threat) {
        drawDefenseOverlay(theme, state.defense)
    }
}

private fun RenderingContext.drawThreatOverlay(
    theme: BaseTheme,
    result: FindWinResult.Win,
    excludedCells: Set<CellCoordinate> = emptySet(),
) {
    result.turns.forEachIndexed { index, (player, cells) ->
        cells.forEach { coordinate ->
            if (coordinate in excludedCells) return@forEach

            val point = layout.run { coordinate.toPixel() }
            val color = theme.playerColor(player)

            drawOverlayTarget(
                point = point,
                color = color,
                backgroundColor = theme.backgroundColor,
                label = "${index + 1}",
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
        excludedCells = defenseCells,
    )

    defense?.forEach { coordinate ->
        val point = layout.run { coordinate.toPixel() }
        drawOverlayTarget(
            point = point,
            color = defenseOverlayColor,
            backgroundColor = theme.backgroundColor,
            label = "+",
        )
    }
}

private fun RenderingContext.drawOverlayTarget(
    point: Point,
    color: Color,
    backgroundColor: Color,
    label: String,
) {
    backend.drawLine(
        from = point,
        to = point,
        stroke = Stroke(backgroundColor.withAlpha(220), (hexSize * 0.78).toFloat()),
    )

    backend.drawLine(
        from = point,
        to = point,
        stroke = Stroke(color.withAlpha(16), (hexSize * 0.78).toFloat()),
        outline = Stroke(color, 4.0.relativeWidth()),
    )

    val backend = backend
    if (backend is CanvasRenderingBackend && label == "+") {
        backend.drawShieldIcon(
            point = point,
            color = color,
            size = hexSize * 0.5,
        )
    } else {
        backend.drawString(
            point = point,
            text = label,
            maxWidth = hexSize * 0.35,
            fontSize = (hexSize * 0.5).toFloat(),
            font = FontType.SansSerifBold,
            color = color,
        )
    }
}

private fun CanvasRenderingBackend.drawShieldIcon(
    point: Point,
    color: Color,
    size: Double,
) {
    val scale = size / 24.0

    canvas.save()
    canvas.translate(point.x - size / 2.0, point.y - size / 2.0)
    canvas.scale(scale, scale)
    canvas.fillStyle = color.css
    canvas.fill(Path2D(SHIELD_ICON_PATH))
    canvas.restore()
}

private fun RenderingContext.drawFirstThreatMoveHighlight(
    theme: BaseTheme,
    threat: FindWinResult.Win,
) {
    val (player, cells) = threat.turns.first()
    val color = theme.playerColor(player)

    cells.forEach { coordinate ->
        val point = layout.run { coordinate.toPixel() }
        val target = point.createHex(hexSize * 0.75)

        backend.drawPolygon(
            shape = target,
            color = color.withAlpha(38),
            outline = Stroke(color.withAlpha(210), 3.0.relativeWidth()),
            borderRadius = 2.5.relativeWidth(),
        )
    }
}
