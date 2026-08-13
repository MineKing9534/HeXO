package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.render.compose.BoardContentBuilder
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.Tooltip
import de.mineking.hexo.web.icons.AlertTriangleIcon
import de.mineking.hexo.web.icons.EyeIcon
import de.mineking.hexo.web.icons.EyeOffIcon
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLCanvasElement

@Composable
fun AnalysedBoardPane(
    board: Board,
    readOnly: Boolean,
    allowAnalyzerOverlay: Boolean,
    turn: AnalyzerTurn?,
    players: Map<CellOwner, GamePlayer>,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: BoardContentBuilder? = null,
) {
    val shouldAnalyze by SettingsKey.SessionAnalyzer.collectAsState()
    val analyzerState = if (shouldAnalyze && turn != null) {
        rememberBoardAnalysis(board, turn)
    } else {
        null
    }

    var showAnalyzerOverlay by remember { mutableStateOf(true) }

    BoardPane(
        board = board,
        readOnly = readOnly,
        viewport = viewport,
        onViewportChange = onViewportChange,
        onBoardInteraction = onBoardInteraction,
        renderingHook = analyzerState?.takeIf { showAnalyzerOverlay && allowAnalyzerOverlay }?.renderingHook(),
        attrs = attrs,
    ) {
        content?.invoke(this)

        if (analyzerState != null && turn != null) {
            AnalyzerStatusDisplay(
                state = analyzerState,
                allowAnalyzerOverlay = allowAnalyzerOverlay,
                showAnalyzerOverlay = showAnalyzerOverlay,
                onShowAnalyzerOverlayChange = { showAnalyzerOverlay = it },
                effectiveTurnPlayer = players[turn.player]!!,
                otherPlayer = players[turn.player.other]!!,
            )
        }
    }
}

@Composable
private fun AnalyzerStatusDisplay(
    state: BoardAnalyzerState,
    allowAnalyzerOverlay: Boolean,
    showAnalyzerOverlay: Boolean,
    onShowAnalyzerOverlayChange: (Boolean) -> Unit,
    effectiveTurnPlayer: GamePlayer,
    otherPlayer: GamePlayer,
) {
    AnalyzerStatusDisplay(
        state = state,
        analyzedPlayer = effectiveTurnPlayer,
        otherPlayer = otherPlayer,
        attrs = { classes("absolute", "right-4", "top-4") },
    ) {
        if (allowAnalyzerOverlay) {
            ActionButton(
                onClick = { onShowAnalyzerOverlayChange(!showAnalyzerOverlay) },
                attrs = { classes("flex-0") },
            ) {
                if (showAnalyzerOverlay) {
                    EyeOffIcon { classes("size-4") }
                } else {
                    EyeIcon { classes("size-4") }
                }
            }
        } else {
            Tooltip(
                text = "The forced-win overlay is disabled for live rated games",
                tooltipAttrs = {
                    classes("right-11", "top-1/2", "w-max", "max-w-72", "-translate-y-1/2")
                },
            ) {
                Div({
                    classes(
                        "size-9", "rounded-md", "border", "shadow-lg", "backdrop-blur-xs", "grid", "place-items-center",
                        "border-amber-400/50", "bg-slate-900/90", "text-amber-300",
                    )
                    attr("role", "img")
                    attr("aria-label", "The forced-win overlay is disabled for live rated games")
                    attr("tabindex", "0")
                    onMouseDown { it.preventDefault() }
                }) {
                    AlertTriangleIcon { classes("size-4") }
                }
            }
        }
    }
}
