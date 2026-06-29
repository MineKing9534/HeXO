package de.mineking.hexo.web.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.hds.asBoard
import de.mineking.hexo.hds.session.LiveSession
import de.mineking.hexo.hds.session.LiveSessionPlayer
import de.mineking.hexo.hds.session.SessionState
import de.mineking.hexo.hds.session.SessionTurn
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.BoardPane
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.cssColor
import kotlinx.browser.window
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration

private const val MOVES_PER_TURN = 2

@Composable
fun SessionBoardPane(session: LiveSession, state: SessionState.InGame?) {
    val move = remember { mutableStateOf(Int.MAX_VALUE) }

    val board = remember(move.value, session) { session.game.asBoard(move.value) }
    val viewport = remember { mutableStateOf<BoardViewport?>(null) }

    val highlightBoard = remember(session.game.id) { mutableStateOf(Board()) }
    val transformedBoard = remember(board, highlightBoard.value) {
        ((board + highlightBoard.value) as MutableBoard).focusWinningRows()
    }

    BoardPane(
        board = transformedBoard,
        viewport = viewport.value,
        onViewportChange = { viewport.value = it },
        onBoardInteraction = { interaction ->
            if (interaction !is BoardInteraction.HighlightBoardInteraction) return@BoardPane
            highlightBoard.value = highlightBoard.value.copy().also {
                interaction.apply(it)
            }
        },
    ) {
        if (state != null) TurnIndicator(session, state)
        BoardControls(session, move, highlightBoard, viewport)
    }
}

@Composable
private fun BoardControls(
    session: LiveSession,
    move: MutableState<Int>,
    highlightBoard: MutableState<Board>,
    viewport: MutableState<BoardViewport?>,
) {
    var move by move
    var highlightBoard by highlightBoard

    ActionButton(
        label = "Move ${min(move, session.game.moves.size)}/${session.game.moves.size}",
        size = ButtonSize.Medium,
        attrs = { classes("absolute", "top-3", "left-3", "z-20", "shadow-lg") },
        onClick = { move = Int.MAX_VALUE },
    )

    Div({ classes("absolute", "bottom-3", "left-3", "z-20", "flex", "gap-3") }) {
        ActionButton(
            label = "Previous",
            enabled = move > 0,
            size = ButtonSize.Medium,
            attrs = { classes("shadow-lg") },
            onClick = { move = max(0, min(move, session.game.moves.size) - 1) },
        )

        ActionButton(
            label = "Next",
            enabled = move < session.game.moves.size,
            size = ButtonSize.Medium,
            attrs = { classes("shadow-lg") },
            onClick = { move = if (move == session.game.moves.size - 1) Int.MAX_VALUE else move + 1 },
        )
    }

    Div({ classes("absolute", "bottom-3", "right-3", "z-20", "flex", "gap-3") }) {
        if (highlightBoard.lineHighlights.isNotEmpty() || highlightBoard.cells.values.any { it.highlight != null }) {
            ActionButton(
                label = "Clear Highlights",
                size = ButtonSize.Medium,
                attrs = { classes("shadow-lg") },
                onClick = { highlightBoard = Board() },
            )
        }
        ActionButton(
            label = "Reset View",
            size = ButtonSize.Medium,
            attrs = { classes("shadow-lg") },
            onClick = { viewport.value = null },
        )
    }
}

@Composable
private fun PlayerTimer(player: LiveSessionPlayer, current: Boolean) {
    val timeRemaining by rememberUpdatedState(player.timeRemaining ?: return)
    var timer by remember { mutableStateOf(timeRemaining.duration) }
    val current by rememberUpdatedState(current)

    DisposableEffect(Unit) {
        fun countdown() {
            val delta = if (current) Clock.System.now() - timeRemaining.timestamp else Duration.ZERO
            timer = maxOf(Duration.ZERO, timeRemaining.duration - delta)
        }

        val interval = window.setInterval(::countdown, 250)
        onDispose { window.clearInterval(interval) }
    }

    Div({
        classes("ml-auto", "font-extrabold", "text-lg")
        if (current) {
            classes("text-emerald-200")
        } else {
            classes("text-slate-200")
        }
    }) {
        val minutes = timer.inWholeMinutes
        val seconds = ceil(timer.inWholeMilliseconds / 1000.0).toInt() % 60
        Text("$minutes:${seconds.toString().padStart(2, '0')}")
    }
}

@Composable
private fun TurnIndicator(session: LiveSession, state: SessionState.InGame) {
    @Composable
    fun PlayerIndicator(player: LiveSessionPlayer) {
        val isCurrentTurn = player === state.currentTurn.player
        Div({ classes("flex", "flex-col", "justify-center", "gap-2") }) {
            Div({
                classes("rounded-md", "py-1", "px-3", "border", "flex", "items-center", "gap-2")
                if (isCurrentTurn) {
                    classes("bg-emerald-500/20", "border-emerald-500/50")
                } else {
                    classes("bg-slate-500/20", "border-slate-500/50")
                }
            }) {
                Div({
                    classes("rounded-full", "size-2", "shrink-0")
                    style { backgroundColor(player.color.cssColor) }
                })
                Span({ classes("max-w-52", "truncate") }) {
                    Text(player.displayName)
                }

                PlayerTimer(player, isCurrentTurn)
            }
            if (isCurrentTurn) TurnIndicator(state.currentTurn)
        }
    }

    Div({ classes("absolute", "top-3", "left-3", "right-3", "flex", "justify-center") }) {
        Div({ classes("shadow-xl", "bg-slate-800", "rounded-lg", "p-3", "max-w-xl", "w-full") }) {
            if (session.tournamentInfo != null) TournamentInfoCard(session)
            Div({ classes("grid", "grid-cols-2", "gap-2", "items-start") }) {
                session.players.forEach {
                    PlayerIndicator(it)
                }
            }
        }
    }
}

@Composable
private fun TournamentInfoCard(session: LiveSession) {
    val tournament = session.tournamentInfo ?: return
    val requiredWins = tournament.bestOf / 2 + 1

    Div({
        classes("rounded-md", "border", "border-slate-700/70", "bg-slate-900/60", "px-3", "py-2", "mb-3")
    }) {
        Div({ classes("mb-1.5", "flex", "items-center", "justify-between", "gap-3", "text-xs") }) {
            Span({ classes("min-w-0", "truncate", "font-semibold", "text-slate-200") }) {
                Text(tournament.tournamentName)
            }
            Span({ classes("shrink-0", "font-medium", "text-slate-400") }) {
                Text("Game ${tournament.currentGameNumber} of ${tournament.bestOf}")
            }
        }

        Div({ classes("grid", "grid-cols-2", "gap-2") }) {
            session.players.forEach { player ->
                Div({ classes("flex", "items-center", "gap-1.5", "text-xs") }) {
                    Div({
                        classes("rounded-full", "size-2", "shrink-0")
                        style { backgroundColor(player.color.cssColor) }
                    })
                    Span({ classes("min-w-0", "flex-1", "truncate", "text-slate-300") }) {
                        Text(player.displayName)
                    }
                    Span({ classes("shrink-0", "tabular-nums") }) {
                        Span({
                            classes("font-bold", "text-sm")
                            if ((player.tournamentMatchWins ?: 0) > 0) {
                                classes("text-emerald-500")
                            } else {
                                classes("text-slate-300")
                            }
                        }) {
                            Text("${player.tournamentMatchWins ?: 0}")
                        }
                        Span({ classes("font-semibold", "text-slate-400", "text-xs") }) {
                            Text("/$requiredWins")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TurnIndicator(turn: SessionTurn) {
    Div({ classes("flex", "gap-1", "flex-row-reverse", "mx-auto", "justify-center") }) {
        repeat(MOVES_PER_TURN) {
            Div({
                classes("rounded-full", "w-6", "h-1.5")
                if (it < turn.placementsRemaining) {
                    style { backgroundColor(turn.player.color.cssColor) }
                } else {
                    classes("bg-slate-500")
                }
            })
        }
    }
}
