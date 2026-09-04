package de.mineking.hexo.web.pages.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameOptions
import de.mineking.hexo.game.model.game.TournamentMatchSnapshot
import de.mineking.hexo.game.model.game.rated
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.page
import de.mineking.hexo.web.board.GameBoardPane
import de.mineking.hexo.web.board.GameBoardViewManager
import de.mineking.hexo.web.board.PlayerName
import de.mineking.hexo.web.board.gamePlayer
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.Anchor
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.BadgeSize
import de.mineking.hexo.web.components.Card
import de.mineking.hexo.web.components.CardHeader
import de.mineking.hexo.web.components.CardLoadingState
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.EmptyStateCard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.Pagination
import de.mineking.hexo.web.components.RatedFilter
import de.mineking.hexo.web.components.RatedFilterControl
import de.mineking.hexo.web.components.ScrollableView
import de.mineking.hexo.web.formatCompact
import de.mineking.hexo.web.icons.CasualGameIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.icons.CloseIcon
import de.mineking.hexo.web.icons.EyeIcon
import de.mineking.hexo.web.icons.StarIcon
import de.mineking.hexo.web.icons.TimeControlIcon
import de.mineking.hexo.web.icons.TournamentIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.map
import de.mineking.hexo.web.rememberHdsRepositories
import de.mineking.hexo.web.rememberQueryParameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLSpanElement
import kotlin.js.Date
import kotlin.time.Duration.Companion.milliseconds

private const val PAGE_SIZE = 10

private class GamePreviewState {
    var selection by mutableStateOf<FinishedGame?>(null)
        private set
    var game by mutableStateOf<FinishedGameWithPosition?>(null)
        private set
    var closing by mutableStateOf(false)
        private set

    suspend fun open(selectedGame: FinishedGame, boardViewManager: GameBoardViewManager) {
        selection = selectedGame
        game = null
        closing = false

        val positionedGame = selectedGame.withPosition()
        if (selection?.id == selectedGame.id) {
            game = positionedGame
            boardViewManager.currentMove = Int.MAX_VALUE
        }
    }

    suspend fun close() {
        closing = true
        delay(180.milliseconds)
        reset()
    }

    fun reset() {
        selection = null
        game = null
        closing = false
    }
}

@InitRoute
fun initLobbyListPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.FinishedGameList))
}

@Page
@Composable
fun GameHistoryPage() {
    val client = rememberHdsRepositories()
    val boardViewManager = rememberHostBoardViewManager<GameBoardViewManager>()
    var page by rememberQueryParameter("page").map(transform = { it?.toIntOrNull() ?: 1 }, transformBack = { it.toString() })
    var filter by rememberQueryParameter("rated").map(
        transform = { RatedFilter.fromQuery(it) },
        transformBack = { it.queryValue },
    )

    if (client == null) {
        LoadingState(filter)
    } else {
        GameList(
            client.finishedGameRepository,
            boardViewManager,
            page,
            filter,
            onPageChange = { page = it },
            onFilterChange = {
                filter = it
                page = 1
            },
        )
    }
}

@Composable
private fun LoadingState(filter: RatedFilter) {
    Div({ classes("lg:h-12") })
    Div({ classes("mx-auto", "flex", "min-h-0", "w-full", "max-w-5xl") }) {
        GameListShell(expanded = false) {
            GameListHeader(filter, onFilterChange = {}, filterEnabled = false)
            CardLoadingState("Loading finished games")
        }
    }
}

@Composable
private fun GameList(
    finishedGameRepository: FinishedGameRepository,
    boardViewManager: GameBoardViewManager,
    page: Int,
    filter: RatedFilter,
    onPageChange: (Int) -> Unit,
    onFilterChange: (RatedFilter) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var games by remember { mutableStateOf(emptyList<FinishedGame>()) }
    var loading by remember { mutableStateOf(true) }
    val preview = remember { GamePreviewState() }

    LaunchedEffect(finishedGameRepository, page, filter) {
        loading = true
        preview.reset()
        games = finishedGameRepository.getGlobalHistory(
            Selector.page(page, PAGE_SIZE)
                .rated(filter.rated),
        ).toList()
        loading = false
    }

    Div({ classes("lg:h-12") })
    Div({
        classes("mx-auto", "flex", "min-h-0", "w-full", "gap-4")
        if (games.isNotEmpty() || preview.selection != null) classes("flex-1")
        if (preview.selection == null) {
            classes("max-w-5xl")
        } else {
            classes("max-w-none", "flex-col", "lg:grid", "lg:grid-cols-[minmax(26rem,1fr)_minmax(0,2fr)]")
        }
    }) {
        GameListCard(
            filter = filter,
            page = page,
            games = games,
            loading = loading,
            previewGame = preview.selection,
            onFilterChange = onFilterChange,
            onPageChange = onPageChange,
            onPreview = { game ->
                coroutineScope.launch {
                    if (preview.selection?.id == game.id) preview.close() else preview.open(game, boardViewManager)
                }
            },
        )

        preview.selection?.let { selectedGame ->
            GamePreview(selectedGame, preview.game, boardViewManager, closing = preview.closing) {
                coroutineScope.launch { preview.close() }
            }
        }
    }
}

@Composable
private fun GameListCard(
    filter: RatedFilter,
    page: Int,
    games: List<FinishedGame>,
    loading: Boolean,
    previewGame: FinishedGame?,
    onFilterChange: (RatedFilter) -> Unit,
    onPageChange: (Int) -> Unit,
    onPreview: (FinishedGame) -> Unit,
) {
    GameListShell(expanded = games.isNotEmpty()) {
        GameListHeader(filter, onFilterChange)
        when {
            loading && games.isEmpty() -> CardLoadingState("Loading finished games")
            games.isEmpty() -> EmptyGameState(filter, page, onPrevious = { onPageChange(page - 1) })
            else -> LoadedGameList(games, page, loading, previewGame, onPageChange, onPreview)
        }
    }
}

@Composable
private fun GameListShell(expanded: Boolean, content: @Composable () -> Unit) {
    Card({
        classes("flex", "min-h-0", "w-full", "flex-col", "gap-4", "overflow-hidden", "p-4")
        if (expanded) classes("flex-1") else classes("shrink-0")
    }) {
        content()
    }
}

@Composable
private fun LoadedGameList(
    games: List<FinishedGame>,
    page: Int,
    loading: Boolean,
    previewGame: FinishedGame?,
    onPageChange: (Int) -> Unit,
    onPreview: (FinishedGame) -> Unit,
) {
    Div({ classes("relative", "flex", "min-h-0", "flex-1", "flex-col", "gap-4") }) {
        ScrollableView({ classes("flex-1", "pr-2") }) {
            Div({
                classes(
                    "overflow-hidden", "rounded-xl", "border", "border-slate-800/80", "bg-slate-950/35",
                    "divide-y", "divide-slate-800/80",
                )
            }) {
                games.forEach { game ->
                    GameRow(game, previewing = game.id == previewGame?.id, onPreview = { onPreview(game) })
                }
            }
        }
        Pagination(page, games.size == PAGE_SIZE, onPageChange = onPageChange)
        if (loading) {
            Div({
                classes(
                    "absolute", "inset-0", "z-10", "grid", "place-items-center", "rounded-xl",
                    "bg-slate-900/70", "backdrop-blur-[2px]",
                )
                attr("aria-label", "Loading games")
            }) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun GameListHeader(filter: RatedFilter, onFilterChange: (RatedFilter) -> Unit, filterEnabled: Boolean = true) {
    Div({ classes("flex", "shrink-0", "flex-wrap", "items-center", "justify-between", "gap-3") }) {
        CardHeader(
            title = "Match history",
            supportingText = "Review recently completed games",
            iconAttrs = { classes("border-sky-400/25", "bg-sky-400/10", "text-sky-300") },
        ) {
            TimeControlIcon { classes("size-4", "fill-none", "stroke-current") }
        }

        Div({ classes("flex", "w-full", "justify-end", "sm:w-auto") }) {
            RatedFilterControl(filter, enabled = filterEnabled, onChange = onFilterChange)
        }
    }
}

@Composable
private fun EmptyGameState(filter: RatedFilter, page: Int, onPrevious: () -> Unit) {
    val description = if (page == 1 && filter != RatedFilter.All) {
        "No ${filter.label.lowercase()} games have been recorded yet."
    } else {
        "There are no games to show on this page."
    }
    EmptyStateCard(
        title = if (page == 1) "No finished games" else "No more games",
        description = description,
        action = if (page > 1) {
            @Composable { ActionButton(label = "Previous page", onClick = onPrevious) }
        } else {
            null
        },
    )
}

@Composable
private fun GameRow(game: FinishedGame, previewing: Boolean, onPreview: () -> Unit) {
    Div({
        classes(
            "relative", "grid", "gap-4", "p-4", "transition-colors", "duration-200", "hover:bg-slate-800/20",
            "md:grid-cols-[minmax(0,1fr)_auto]", "md:items-center",
        )
        if (previewing) {
            classes(
                "bg-slate-800/70", "shadow-[inset_0_1px_0_rgb(255_255_255/0.04)]", "hover:bg-slate-800/80",
            )
            attr("aria-label", "Currently previewing this game")
        }
    }) {
        if (previewing) {
            GameRowPreviewIndicator()
        }
        Div({ classes("min-w-0") }) {
            Div({ classes("flex", "flex-wrap", "items-center", "gap-2") }) {
                game.players.forEachIndexed { index, player ->
                    if (index > 0) {
                        Span({ classes("text-xs", "font-medium", "text-slate-500") }) {
                            Text("vs")
                        }
                    }
                    PlayerName(player.gamePlayer) {
                        classes("font-semibold")
                        if (player == game.result.winner) classes("font-bold", "text-emerald-200!")
                    }
                }
            }
            P({ classes("mt-1", "truncate", "text-xs", "font-mono", "text-slate-600") }) {
                Text(game.id.value)
            }

            Div({ classes("mt-3", "flex", "flex-wrap", "items-center", "gap-2") }) {
                GameTypeBadge(game.options, game.tournament)
                Badge(attrs = {
                    attr("title", game.startedAt.toString())
                }) {
                    Text(Date(game.startedAt.toEpochMilliseconds().toDouble()).formatMinutePrecision())
                }
                Badge {
                    Text(game.result.duration.formatCompact())
                }
                Badge {
                    Text("${game.moveCount} moves")
                }
            }
        }

        GameRowActions(game, previewing, onPreview)
    }
}

@Composable
private fun GameRowPreviewIndicator() {
    Span({
        classes(
            "absolute", "inset-y-0", "left-0", "w-1", "bg-linear-to-b", "from-sky-300", "to-sky-500",
            "shadow-sm", "shadow-sky-500/30",
        )
        attr("aria-hidden", "true")
    })
}

@Composable
private fun GameRowActions(game: FinishedGame, previewing: Boolean, onPreview: () -> Unit) {
    Div({ classes("flex", "w-full", "flex-col", "items-stretch", "justify-end", "gap-2", "md:w-auto") }) {
        Anchor(AppRoute.FinishedGame(game.id), {
            classes(
                "group", "inline-flex", "shrink-0", "items-center", "justify-center", "gap-2", "rounded-lg",
                "border", "border-slate-700", "bg-slate-800/70", "px-4", "py-2", "text-sm", "font-semibold",
                "text-slate-300", "transition", "hover:border-slate-600", "hover:bg-slate-700/70",
                "hover:text-slate-100", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
            )
        }) {
            Text("Review game")
            ChevronRightIcon {
                classes("size-4", "shrink-0", "transition-transform", "group-hover:translate-x-0.5")
            }
        }
        Button({
            classes(
                "group", "hidden", "w-full", "cursor-pointer", "items-center", "justify-center", "gap-2",
                "md:inline-flex",
                "rounded-lg", "border", "px-4", "py-2", "text-sm", "font-semibold", "transition",
                "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-sky-400/50",
            )
            if (previewing) {
                classes("border-sky-400/40", "bg-sky-500/15", "text-sky-200", "hover:bg-sky-500/20")
            } else {
                classes(
                    "border-slate-700", "bg-slate-950/60", "text-slate-400", "hover:border-slate-600",
                    "hover:bg-slate-800/70", "hover:text-slate-100",
                )
            }
            onClick { onPreview() }
        }) {
            Text(if (previewing) "Hide preview" else "Preview")
            EyeIcon { classes("size-4", "shrink-0") }
        }
    }
}

@Composable
private fun GamePreview(
    game: FinishedGame,
    positionedGame: FinishedGameWithPosition?,
    boardViewManager: GameBoardViewManager,
    closing: Boolean,
    onClose: () -> Unit,
) {
    Div({
        classes("hidden", "min-h-0", "min-w-0", "game-preview-enter", "md:flex")
        if (closing) classes("game-preview-exit")
    }) {
        if (positionedGame == null) {
            Div({
                classes(
                    "relative", "grid", "min-h-96", "min-w-0", "flex-1", "place-items-center", "rounded-2xl",
                    "border", "border-slate-800", "bg-slate-900", "shadow-2xl",
                )
                attr("aria-label", "Loading game preview")
            }) {
                LoadingIndicator()
                GamePreviewHeader(game, onClose)
            }
        } else {
            GameBoardPane(game = positionedGame, isLive = false, plain = true, boardViewManager = boardViewManager) {
                GamePreviewHeader(game, onClose)
            }
        }
    }
}

@Composable
private fun GamePreviewHeader(game: FinishedGame, onClose: () -> Unit) {
    Div({
        classes(
            "absolute", "left-3", "top-3", "z-20", "min-w-0", "max-w-[calc(100%-4.5rem)]",
            "rounded-lg", "border", "border-slate-700/80", "bg-slate-950/85", "px-3", "py-2",
            "shadow-lg", "shadow-black/25", "backdrop-blur-sm",
        )
    }) {
        Div({ classes("mb-0.5", "flex", "items-center", "gap-1.5") }) {
            EyeIcon { classes("size-3", "text-sky-300") }
            Span({ classes("text-[0.65rem]", "font-bold", "tracking-widest", "text-sky-300", "uppercase") }) {
                Text("Previewing")
            }
        }
        H2({ classes("truncate", "text-sm", "font-semibold", "text-slate-100") }) {
            Text(game.players.joinToString(" vs ") { it.displayName })
        }
    }

    Button({
        attr("aria-label", "Hide preview")
        classes(
            "grid", "size-8", "shrink-0", "cursor-pointer", "place-items-center", "rounded-md",
            "text-slate-400", "transition", "hover:bg-slate-800", "hover:text-rose-400",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-rose-400/50",
            "absolute", "top-2", "right-2",
        )
        onClick { onClose() }
    }) {
        CloseIcon()
    }
}

@Composable
fun GameTypeBadge(
    options: GameOptions,
    tournament: TournamentMatchSnapshot?,
    size: BadgeSize = BadgeSize.Small,
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
) {
    Badge(
        color = when {
            tournament != null -> Color.Sky
            options.rated -> Color.Yellow
            else -> Color.Emerald
        },
        attrs = attrs,
        size = size,
    ) {
        when {
            tournament != null -> {
                TournamentIcon { classes("size-4", "text-current") }
                Text("Tournament")
            }
            options.rated -> {
                StarIcon { classes("size-4", "text-amber-300") }
                Text("Rated")
            }
            else -> {
                CasualGameIcon { classes("size-4", "text-current") }
                Text("Casual")
            }
        }
    }
}

private fun Date.formatMinutePrecision(): String {
    val hours = getHours().toString().padStart(2, '0')
    val minutes = getMinutes().toString().padStart(2, '0')
    return "${toLocaleDateString()}, $hours:$minutes"
}
