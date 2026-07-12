package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardScope
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.compose.DEFAULT_CELL_HOVER_COlOR
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.tint
import de.mineking.hexo.board.render.image.theme.withAlpha
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.playerColor
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.rememberSettingsValue
import de.mineking.hexo.web.worker.AnalysisInput
import de.mineking.hexo.web.worker.AnalysisWorker
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

typealias BoardPaneContentBuilder = @Composable BoardScope.(AnalyserResult?) -> Unit

private val opponentWinOverlayTint = Color.rgba(0x55ff0000)
private val defenseOverlayColor = Color.rgb(0x15601c)

@Composable
fun BoardPane(
    board: Board,
    readOnly: Boolean,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    analyseAs: AnalyserTurn? = null,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: BoardPaneContentBuilder? = null,
) {
    if (AppGlobals.isExporting) {
        Div({
            classes(
                "grid", "grow", "place-items-center", "rounded-2xl", "border", "border-slate-800",
                "bg-linear-to-br", "from-slate-900", "to-slate-900/30", "shadow-2xl", "shadow-black/30", "h-full",
            )
        }) {
            LoadingIndicator { classes("size-9") }
        }
    }

    val analyserResult = when (analyseAs) {
        null -> null
        else -> rememberAnalyserResult(board, analyseAs)
    }

    val readOnlyBoardHoverIndicator by rememberSettingsValue(SettingsKey.ReadOnlyBoardHoverIndicator)
    val theme by rememberTheme()

    Div({
        classes(
            "relative", "min-h-0", "min-w-0", "flex-1", "overflow-hidden", "rounded-2xl",
            "border", "border-slate-800", "bg-slate-900", "shadow-2xl",
        )
    }) {
        InteractiveBoard(
            board = board,
            viewport = viewport,
            onViewportChange = onViewportChange,
            onBoardInteraction = onBoardInteraction,
            theme = theme,
            cellHoverColor = DEFAULT_CELL_HOVER_COlOR.takeIf { readOnlyBoardHoverIndicator || !readOnly },
            middleLayer = {
                val result = analyserResult ?: return@InteractiveBoard
                drawAnalyserOverlay(theme, result)
            },
            attrs = {
                attr("width", "1200")
                attr("height", "900")
                classes("block", "h-full", "w-full", "touch-none")
                attrs?.invoke(this)
            },
        ) {
            content?.invoke(this, analyserResult)
        }

        @Composable
        fun Edge(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
            Div({
                style {
                    variable("--hexo-background", theme.backgroundColor.toString())
                }
                classes("pointer-events-none", "absolute", "z-10", "from-(--hexo-background)", "via-transparent", "to-transparent")
                attrs?.invoke(this)
            })
        }

        Edge { classes("inset-x-0", "top-0", "h-4", "bg-linear-to-b") }
        Edge { classes("inset-x-0", "bottom-0", "h-4", "bg-linear-to-t") }
        Edge { classes("inset-y-0", "left-0", "w-4", "bg-linear-to-r") }
        Edge { classes("inset-y-0", "right-0", "w-4", "bg-linear-to-l") }
    }
}

data class AnalyserTurn(val player: CellOwner, val remaining: Int)

data class AnalyserResult(
    val threat: FindWinResult,
    val defense: FindDefenseResult,
)

private fun RenderingContext.drawAnalyserOverlay(theme: BaseTheme, result: AnalyserResult) {
    if (result.threat is FindWinResult.Win) {
        drawThreatOverlay(theme, result.threat, self = true)
    } else if (result.defense is FindDefenseResult.Threat) {
        drawDefenseOverlay(theme, result.defense)
    }
}

private fun RenderingContext.drawThreatOverlay(theme: BaseTheme, result: FindWinResult.Win, self: Boolean) {
    result.turns.forEachIndexed { index, (player, cells) ->
        cells.forEach { cell ->
            val point = layout.run { cell.toPixel() }

            val color = theme.playerColor(player).withAlpha(128)
            val effectiveColor = if (self) color else color.tint(opponentWinOverlayTint)

            backend.drawString(point, "${index + 1}", Double.MAX_VALUE, hexSize.toFloat() * 0.7f, FontType.SansSerifBold, effectiveColor)
        }
    }
}

private fun RenderingContext.drawDefenseOverlay(theme: BaseTheme, result: FindDefenseResult.Threat) {
    val defense = result.defenses.firstOrNull()
    defense?.forEach { cell ->
        val point = layout.run { cell.toPixel() }
        val hex = point.createHex(hexSize)

        backend.drawPolygon(hex, defenseOverlayColor.withAlpha(64), Stroke(
            color = defenseOverlayColor,
            width = 4.0.relativeWidth(),
        ))
    }

    drawThreatOverlay(theme, result.threat, self = false)
}

@Composable
private fun rememberAnalyserResult(board: Board, turn: AnalyserTurn): AnalyserResult? {
    val boardOwners = remember(board) {
        board.cells.mapNotNull { (coordinate, cell) ->
            val owner = cell.owner ?: return@mapNotNull null
            coordinate to owner
        }.sortedWith(compareBy({ it.first.q }, { it.first.r }, { it.second.name }))
    }

    var result by remember(boardOwners, turn) { mutableStateOf<AnalyserResult?>(null) }
    var requestId by remember { mutableStateOf(0) }

    LaunchedEffect(boardOwners, turn) {
        val currentRequestId = ++requestId

        val worker = AnalysisWorker { output ->
            if (output.requestId == requestId) {
                result = AnalyserResult(output.threat, output.defense)
            }
        }

        try {
            worker.postInput(AnalysisInput(
                requestId = currentRequestId,
                board = board,
                player = turn.player,
                remaining = turn.remaining,
            ))
            awaitCancellation()
        } finally {
            worker.terminate()
        }
    }

    return result
}
