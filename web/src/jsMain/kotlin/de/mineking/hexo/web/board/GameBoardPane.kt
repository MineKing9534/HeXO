package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.DEFAULT_MOVES_PER_TURN
import de.mineking.hexo.board.Move
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.moves
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardScope
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.image.div
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.game.FinishedGamePlayer
import de.mineking.hexo.game.model.game.Game
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.playerWithColor
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.tournament.requiredWins
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.playerCssColor
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.browser.window
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun GameBoardPane(game: GameWithPosition, isLive: Boolean, boardViewManager: GameBoardViewManager) {
    val viewport = remember { mutableStateOf(BoardViewport()) }

    val watchPartyController = rememberWatchPartyController()
    LaunchedEffect(game.id) {
        viewport.value = BoardViewport()

        // Don't clear highlights when subscribed to a watch party
        if (watchPartyController.subscribedWatchParty !is EntityState.Data<*>) {
            boardViewManager.clearHighlights()
        }
    }

    val position = game.rememberPosition(boardViewManager.currentMove)
    val (effectiveTurnPlayer, effectivePlacementsRemaining) = position.nextTurn
    val allowAnalyzerOverlay = !(isLive && game.options.rated)

    val shouldAnalyze by SettingsKey.SessionAnalyzer.collectAsState()
    val analyzerTurn = if (shouldAnalyze && (isLive || boardViewManager.currentMove < game.moveCount)) {
        AnalyzerTurn(effectiveTurnPlayer, effectivePlacementsRemaining)
    } else {
        null
    }

    AnalysedBoardPane(
        boardViewManager = boardViewManager.transformBoard(game to boardViewManager.currentMove) { overlay ->
            val board = game.position.toBoard(focusWinningRows = false)
            (board + overlay).focusWinningRows()
        },
        readOnly = true,
        allowAnalyzerOverlay = allowAnalyzerOverlay,
        turn = analyzerTurn,
        players = game.players.associate { it.color to it.gamePlayer },
        viewport = viewport.value,
        onViewportChange = { viewport.value = it },
        onBoardInteraction = { interaction ->
            if (interaction !is BoardInteraction.HighlightBoardInteraction) return@AnalysedBoardPane
            boardViewManager.apply(interaction)
        },
    ) {
        TurnIndicator(game, isLive, game.playerWithColor(effectiveTurnPlayer), effectivePlacementsRemaining)
        BoardControls(game, boardViewManager, viewport)
    }
}

@Composable
private fun BoardScope.BoardControls(
    game: GameWithPosition,
    boardViewManager: GameBoardViewManager,
    viewport: MutableState<BoardViewport>,
) {
    val totalMoves = game.moveCount

    MoveKeyboardShortcuts(
        boardViewManager = boardViewManager,
        moves = game.position.moves,
        viewport = viewport,
    )

    Div({ classes("absolute", "top-3", "left-3", "z-20") }) {
        BoardActionButton(onClick = { boardViewManager.currentMove = Int.MAX_VALUE }) {
            Text("Move ${min(boardViewManager.currentMove, totalMoves)}/$totalMoves")
        }
    }

    Div({ classes("absolute", "bottom-3", "left-3", "z-20", "flex", "gap-3") }) {
        BoardActionButton(
            enabled = boardViewManager.currentMove > 0,
            attrs = {
                attr("aria-label", "Previous move")
                attr("title", "Previous move")
            },
            onClick = { boardViewManager.currentMove = previousMove(boardViewManager.currentMove, totalMoves) },
        ) { ChevronLeftIcon { classes("size-4") } }

        BoardActionButton(
            enabled = boardViewManager.currentMove < totalMoves,
            attrs = {
                attr("aria-label", "Next move")
                attr("title", "Next move")
            },
            onClick = { boardViewManager.currentMove = nextMove(boardViewManager.currentMove, totalMoves) },
        ) { ChevronRightIcon { classes("size-4") } }
    }
}

@Composable
private fun BoardScope.MoveKeyboardShortcuts(
    boardViewManager: GameBoardViewManager,
    moves: List<Move>,
    viewport: MutableState<BoardViewport>,
) {
    val totalMoves by rememberUpdatedState(moves.size)
    val currentMoves by rememberUpdatedState(moves)
    val currentRenderLayout by rememberUpdatedState(renderLayout)

    DisposableEffect(boardViewManager.currentMove) {
        val keyDown = EventListener { event ->
            if (event !is KeyboardEvent) return@EventListener
            if (event.altKey || event.ctrlKey || event.metaKey) return@EventListener
            if ((event.target as? HTMLElement)?.isContentEditable == true) return@EventListener

            when (event.key) {
                "ArrowLeft" -> {
                    event.preventDefault()
                    boardViewManager.currentMove = previousMove(boardViewManager.currentMove, totalMoves)
                }
                "ArrowRight" -> {
                    event.preventDefault()
                    boardViewManager.currentMove = nextMove(boardViewManager.currentMove, totalMoves)
                }
                in (1..9).map { "$it" } -> {
                    val shortcut = event.key.toInt()
                    val max = boardViewManager.currentMove.coerceIn(0, currentMoves.size)
                    if (shortcut > max) return@EventListener

                    val coordinate = currentMoves[max - shortcut].coordinate
                    val point = currentRenderLayout.size.run { coordinate.toPixel() }

                    val current = viewport.value
                    viewport.value = current.copy(center = point / current.zoom)
                }
            }
        }

        window.addEventListener("keydown", keyDown)
        onDispose { window.removeEventListener("keydown", keyDown) }
    }
}

private fun previousMove(move: Int, totalMoves: Int) = max(0, min(move, totalMoves) - 1)
private fun nextMove(move: Int, totalMoves: Int) = if (move >= totalMoves - 1) Int.MAX_VALUE else move + 1

@Composable
private fun PlayerTimer(player: LiveSessionPlayer, current: Boolean) {
    val soundPlayer = rememberSoundPlayer()

    val timeRemaining by rememberUpdatedState(player.timeRemaining ?: return)
    var timer by remember { mutableStateOf(timeRemaining.duration) }
    val current by rememberUpdatedState(current)

    val playerTimerSound by SettingsKey.SessionViewTimerSounds.collectAsState()

    DisposableEffect(Unit) {
        fun countdown() {
            val delta = if (current) Clock.System.now() - timeRemaining.timestamp else Duration.ZERO
            timer = maxOf(Duration.ZERO, timeRemaining.duration - delta)
        }

        val interval = window.setInterval(::countdown, 250)
        onDispose { window.clearInterval(interval) }
    }

    LaunchedEffect(timer.inWholeSeconds) {
        if (playerTimerSound && timer <= 10.seconds) soundPlayer.play(SoundEffect.CountdownWarning)
    }

    Div({
        classes("font-extrabold", "text-lg", "tabular-nums")
        if (current) {
            classes("text-emerald-200")
        } else {
            classes("text-slate-200")
        }
    }) {
        val totalSeconds = ceil(timer.inWholeMilliseconds / 1000.0).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        Text("$minutes:${seconds.toString().padStart(2, '0')}")
    }
}

@Composable
private fun TurnIndicator(
    game: Game,
    isLive: Boolean,
    currentPlayer: Player,
    placementsRemaining: Int,
) {
    @Composable
    fun PlayerIndicator(player: Player) {
        val isCurrentTurn = player === currentPlayer
        Div({ classes("flex", "flex-col", "justify-center", "gap-2") }) {
            Div({
                classes("rounded-md", "py-1", "px-3", "border", "flex", "items-center", "justify-between")
                if (isCurrentTurn) {
                    classes("bg-emerald-500/20", "border-emerald-500/50")
                } else {
                    classes("bg-slate-500/20", "border-slate-500/50")
                }
            }) {
                Player(player.gamePlayer)
                if (player is LiveSessionPlayer && isLive) PlayerTimer(player, isCurrentTurn)
            }
            if (isCurrentTurn) PlacementsRemainingIndicator(player.color, placementsRemaining)
        }
    }

    Div({ classes("pointer-events-none", "absolute", "top-3", "left-3", "right-3", "flex", "justify-center") }) {
        Div({
            classes("pointer-events-auto", "shadow-xl", "bg-slate-800/85", "rounded-lg", "p-3", "max-w-xl", "w-full", "backdrop-blur-xs")
        }) {
            if (game.tournament != null) TournamentInfoCard(game)
            if (game.options.rated) RatedInfoCard(game)

            Div({ classes("grid", "grid-cols-2", "gap-2", "items-start") }) {
                game.players.forEach {
                    PlayerIndicator(it)
                }
            }
        }
    }
}

@Composable
private fun RatedInfoCard(game: Game) {
    Div({
        classes("rounded-md", "border", "border-slate-700/70", "bg-slate-900/60", "px-3", "py-2", "mb-3")
    }) {
        Div({ classes("mb-1.5", "flex", "items-center", "justify-center", "text-xs") }) {
            Span({ classes("min-w-0", "truncate", "font-semibold", "text-slate-400", "uppercase") }) {
                Text("Rated Match")
            }
        }

        Div({ classes("grid", "grid-cols-2", "gap-2") }) {
            game.players.forEach { player ->
                Div({ classes("flex", "items-center", "gap-1.5", "text-xs") }) {
                    Player(player.gamePlayer)
                    Span({ classes("text-sm", "font-medium", "text-slate-300") }) {
                        Text("Elo ${player.elo}")

                        @Composable
                        fun EloDiff(diff: Int) {
                            Span({
                                classes(if (diff >= 0) "text-emerald-300" else "text-rose-300")
                            }) {
                                if (diff >= 0) Text("+")
                                Text("$diff")
                            }
                        }

                        Span({ classes("ml-1", "text-xs") }) {
                            if (player is LiveSessionPlayer) {
                                EloDiff(player.ratingAdjustment?.eloGain ?: 0)
                                Text("/")
                                EloDiff(player.ratingAdjustment?.eloLoss ?: 0)
                            } else if (player is FinishedGamePlayer) {
                                EloDiff(player.eloChange ?: 0)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentInfoCard(game: Game) {
    val tournament = game.tournament ?: return

    Div({
        classes("rounded-md", "border", "border-slate-700/70", "bg-slate-900/60", "px-3", "py-2", "mb-3")
    }) {
        Div({ classes("mb-1.5", "flex", "items-center", "justify-between", "gap-3", "text-xs") }) {
            Span({ classes("min-w-0", "truncate", "font-semibold", "text-slate-200") }) {
                Text(tournament.tournament.info.name)
            }
            Span({ classes("shrink-0", "font-medium", "text-slate-400") }) {
                Text("Game ${tournament.matchInfo.currentGameNumber} of ${tournament.matchInfo.bestOf}")
            }
        }

        Div({ classes("grid", "grid-cols-2", "gap-2") }) {
            game.players.forEach { player ->
                Div({ classes("flex", "items-center", "gap-1.5", "text-xs") }) {
                    Player(player.gamePlayer)
                    Span({ classes("shrink-0", "tabular-nums") }) {
                        Span({
                            classes("font-bold", "text-sm")
                            classes(if ((player.tournamentMatchWins ?: 0) == tournament.matchInfo.requiredWins - 1) {
                                "text-emerald-500"
                            } else {
                                "text-slate-300"
                            })
                        }) {
                            Text("${player.tournamentMatchWins ?: 0}")
                        }
                        Span({ classes("font-semibold", "text-slate-400", "text-xs") }) {
                            Text("/${tournament.matchInfo.requiredWins}")
                        }
                    }
                }
            }
        }
    }
}

data class GamePlayer(val displayName: String, val color: CellOwner)
val Player.gamePlayer get() = GamePlayer(displayName, color)

@Composable
fun Player(
    player: GamePlayer,
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
) {
    val theme by rememberTheme()
    Span({
        classes("inline-flex", "gap-1.5", "flex-nowrap", "items-center", "text-slate-300")
        attrs?.invoke(this)
    }) {
        Div({
            classes("rounded-full", "size-2", "shrink-0")
            style { backgroundColor(theme.playerCssColor(player.color)) }
        })
        Span({ classes("min-w-0", "max-w-52", "flex-1", "truncate") }) {
            Text(player.displayName)
        }
    }
}

@Composable
private fun PlacementsRemainingIndicator(color: CellOwner, remaining: Int) {
    val theme by rememberTheme()
    Div({ classes("flex", "gap-1", "flex-row-reverse", "mx-auto", "justify-center") }) {
        repeat(DEFAULT_MOVES_PER_TURN) {
            Div({
                classes("rounded-full", "w-6", "h-1.5")
                if (it < remaining) {
                    style { backgroundColor(theme.playerCssColor(color)) }
                } else {
                    classes("bg-slate-500")
                }
            })
        }
    }
}
