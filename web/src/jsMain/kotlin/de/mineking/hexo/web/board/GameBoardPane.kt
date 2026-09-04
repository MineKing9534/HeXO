package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.DEFAULT_MOVES_PER_TURN
import de.mineking.hexo.board.Move
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.moves
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardContentBuilder
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardScope
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.game.FinishedGamePlayer
import de.mineking.hexo.game.model.game.Game
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.playerWithColor
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.tournament.requiredWins
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.playerCssColor
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.browser.window
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
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

@Composable
fun GameBoardPane(
    game: GameWithPosition,
    isLive: Boolean,
    plain: Boolean = false,
    boardViewManager: GameBoardViewManager,
    content: BoardContentBuilder? = null,
) {
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
    val playerTimeProvider = rememberPlayerTimeProvider(game, boardViewManager.currentMove)
    val allowAnalyzerOverlay = !(isLive && game.options.rated)

    val shouldAnalyze by SettingsKey.SessionAnalyzer.collectAsState()
    val analyzerTurn = if (shouldAnalyze && (isLive || boardViewManager.currentMove < game.moveCount)) {
        AnalyzerTurn(effectiveTurnPlayer, effectivePlacementsRemaining)
    } else {
        null
    }

    AnalysedBoardPane(
        boardViewManager = boardViewManager.transformBoard(game to boardViewManager.currentMove) { overlay ->
            val board = position.toBoard(focusWinningRows = false)
            (board + overlay).focusWinningRows()
        },
        readOnly = true,
        plain = plain,
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
        content?.invoke(this)

        if (!plain) {
            TurnIndicator(
                game,
                game.playerWithColor(effectiveTurnPlayer),
                effectivePlacementsRemaining,
                playerTimeProvider,
            )
            BoardControls(game, boardViewManager, viewport)
        }
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
        MoveIndicatorButton(
            currentMove = min(boardViewManager.currentMove, totalMoves),
            totalMoves = totalMoves,
            onClick = { boardViewManager.currentMove = Int.MAX_VALUE },
        )
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
private fun MoveIndicatorButton(currentMove: Int, totalMoves: Int, onClick: () -> Unit) {
    val reviewingHistory = currentMove < totalMoves
    Button({
        classes(
            "inline-flex", "h-9", "cursor-pointer", "items-center", "gap-2", "rounded-lg", "border",
            "bg-slate-950/75", "px-2.5", "shadow-md", "backdrop-blur-xs",
            "transition-colors", "hover:bg-slate-900/85",
        )
        if (reviewingHistory) {
            classes("border-amber-400/55", "hover:border-amber-300/75")
            attr("title", "Return to latest move")
        } else {
            classes("border-slate-500/60", "hover:border-slate-400/75")
            attr("title", "Latest move")
        }
        attr("aria-label", if (reviewingHistory) "Return to latest move" else "Latest move")
        onClick { onClick() }
    }) {
        Span({ classes("text-[10px]", "font-semibold", "uppercase", "tracking-[0.16em]", "text-slate-400") }) {
            Text("Move")
        }
        Span({ classes("flex", "items-baseline", "gap-1", "tabular-nums") }) {
            Span({ classes("text-sm", "font-extrabold", if (reviewingHistory) "text-amber-200" else "text-slate-100") }) {
                Text("$currentMove")
            }
            Span({ classes("text-[10px]", "font-medium", "text-slate-600") }) { Text("/") }
            Span({ classes("text-xs", "font-semibold", "text-slate-400") }) { Text("$totalMoves") }
        }
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
            val numberShortcut = event.numberShortcut

            when {
                event.key == "ArrowLeft" -> {
                    event.preventDefault()
                    boardViewManager.currentMove = previousMove(boardViewManager.currentMove, totalMoves)
                }
                event.key == "ArrowRight" -> {
                    event.preventDefault()
                    boardViewManager.currentMove = nextMove(boardViewManager.currentMove, totalMoves)
                }
                numberShortcut != null -> {
                    event.preventDefault()
                    val max = boardViewManager.currentMove.coerceIn(0, currentMoves.size)
                    if (numberShortcut > max) return@EventListener

                    val coordinate = currentMoves[max - numberShortcut].coordinate
                    val point = currentRenderLayout.size.run { coordinate.toPixel() }

                    val current = viewport.value
                    viewport.value = current.copy(center = point)
                }
            }
        }

        window.addEventListener("keydown", keyDown)
        onDispose { window.removeEventListener("keydown", keyDown) }
    }
}

private val KeyboardEvent.numberShortcut: Int?
    get() = when {
        code.startsWith("Digit") -> code.removePrefix("Digit").toIntOrNull()
        code.startsWith("Numpad") -> code.removePrefix("Numpad").toIntOrNull()
        else -> key.toIntOrNull()
    }?.takeIf { it in 1..9 }

private fun previousMove(move: Int, totalMoves: Int) = max(0, min(move, totalMoves) - 1)
private fun nextMove(move: Int, totalMoves: Int) = if (move >= totalMoves - 1) Int.MAX_VALUE else move + 1

@Composable
private fun PlayerTimer(player: Player, current: Boolean, timeProvider: PlayerTimeProvider) {
    val timer = timeProvider.remainingTime(player, current) ?: return
    Div({
        classes("font-extrabold", "text-lg", "leading-none", "tabular-nums")
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
    currentPlayer: Player,
    placementsRemaining: Int,
    timeProvider: PlayerTimeProvider,
) {
    @Composable
    fun PlayerIndicator(player: Player) {
        val isCurrentTurn = player === currentPlayer
        Div({ classes("flex", "flex-col", "justify-center", "gap-2") }) {
            Div({
                classes(
                    "h-9", "min-w-0", "rounded-lg", "border-2", "px-2.5", "flex", "items-center", "justify-between", "gap-2",
                    "bg-slate-900/75", "backdrop-blur-xs",
                )
                if (isCurrentTurn) {
                    classes("border-emerald-400/70", "shadow-[inset_0_0_18px_rgb(16_185_129/0.08)]")
                } else {
                    classes("border-slate-500/60")
                }
            }) {
                PlayerName(player.gamePlayer) {
                    classes(
                        "min-w-0", "text-[11px]", "font-semibold", "uppercase", "tracking-[0.14em]",
                        if (isCurrentTurn) "text-slate-100" else "text-slate-300",
                    )
                }
                PlayerTimer(player, isCurrentTurn, timeProvider)
            }
            if (isCurrentTurn) PlacementsRemainingIndicator(player.color, placementsRemaining)
        }
    }

    Div({ classes("pointer-events-none", "absolute", "top-3", "left-3", "right-3", "mx-auto", "max-w-xl") }) {
        if (game.tournament != null) TournamentInfoCard(game)
        if (game.options.rated) RatedInfoCard(game)

        Div({ classes("grid", "grid-cols-2", "gap-2", "items-start") }) {
            game.players.forEach {
                PlayerIndicator(it)
            }
        }
    }
}

@Composable
private fun RatedInfoCard(game: Game) {
    HudInfoCard(
        accent = "border-amber-400/35",
        header = {
            HudInfoTitle("bg-amber-300", "text-amber-200", "Rated match")
            Span({ classes("shrink-0", "text-[10px]", "font-semibold", "uppercase", "tracking-wider", "text-slate-500") }) {
                Text("ELO")
            }
        },
    ) {
        Div({ classes("grid", "grid-cols-2", "gap-2") }) {
            game.players.forEach { player ->
                HudPlayerRow(player) {
                    RatedPlayerElo(player)
                }
            }
        }
    }
}

@Composable
private fun TournamentInfoCard(game: Game) {
    val tournament = game.tournament ?: return

    HudInfoCard(
        accent = "border-sky-400/35",
        header = {
            Div({ classes("min-w-0") }) {
                HudInfoTitle("bg-sky-300", "text-sky-200", "Tournament")
                Span({ classes("mt-0.5", "block", "truncate", "text-xs", "font-semibold", "text-slate-200") }) {
                    Text(tournament.tournament.info.name)
                }
            }
            Span({
                classes(
                    "shrink-0", "rounded-md", "border", "border-slate-600/60", "bg-slate-950/45",
                    "px-2", "py-1", "text-[10px]", "font-semibold", "text-slate-400",
                )
            }) {
                Text("Game ${tournament.matchInfo.currentGameNumber}/${tournament.matchInfo.bestOf}")
            }
        },
    ) {
        Div({ classes("grid", "grid-cols-2", "gap-2") }) {
            game.players.forEach { player ->
                HudPlayerRow(player) {
                    Span({ classes("shrink-0", "tabular-nums") }) {
                        Span({
                            classes("text-sm", "font-extrabold")
                            classes(if ((player.tournamentMatchWins ?: 0) == tournament.matchInfo.requiredWins - 1) {
                                "text-emerald-300"
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

@Composable
private fun HudInfoCard(accent: String, header: @Composable () -> Unit, content: @Composable () -> Unit) {
    Div({
        classes(
            "mb-3", "overflow-hidden", "rounded-lg", "border-2", accent, "bg-slate-900/75",
            "backdrop-blur-xs",
        )
    }) {
        Div({ classes("flex", "min-h-9", "items-center", "justify-between", "gap-3", "border-b", "border-slate-700/55", "px-3", "py-2") }) {
            header()
        }
        Div({ classes("p-2") }) { content() }
    }
}

@Composable
private fun HudInfoTitle(dotColor: String, textColor: String, title: String) {
    Div({ classes("flex", "min-w-0", "items-center", "gap-2") }) {
        Span({ classes("size-1.5", "shrink-0", "rounded-full", dotColor) })
        Span({ classes("truncate", "text-[10px]", "font-bold", "uppercase", "tracking-[0.16em]", textColor) }) {
            Text(title)
        }
    }
}

@Composable
private fun HudPlayerRow(player: Player, content: @Composable () -> Unit) {
    Div({
        classes(
            "flex", "min-w-0", "items-center", "justify-between", "gap-2", "rounded-md", "border",
            "border-white/5", "bg-slate-950/35", "px-2", "py-1.5",
        )
    }) {
        PlayerName(player.gamePlayer) {
            classes("min-w-0", "text-[10px]", "font-semibold", "uppercase", "tracking-wider", "text-slate-300")
        }
        content()
    }
}

@Composable
private fun RatedPlayerElo(player: Player) {
    Span({ classes("shrink-0", "text-xs", "font-semibold", "tabular-nums", "text-slate-300") }) {
        Text("${player.elo}")
        when (player) {
            is LiveSessionPlayer -> player.ratingAdjustment?.let {
                RatingChange(it.eloGain)
                Span({ classes("mx-0.5", "text-slate-600") }) { Text("/") }
                RatingChange(it.eloLoss)
            }
            is FinishedGamePlayer -> RatingChange(player.eloChange)
            else -> Unit
        }
    }
}

@Composable
private fun RatingChange(value: Int?) {
    value ?: return
    Span({ classes("ml-1", if (value >= 0) "text-emerald-300" else "text-rose-300") }) {
        Text(if (value > 0) "+$value" else "$value")
    }
}

data class GamePlayer(val displayName: String, val color: CellOwner)
val Player.gamePlayer get() = GamePlayer(displayName, color)

@Composable
fun PlayerName(
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
