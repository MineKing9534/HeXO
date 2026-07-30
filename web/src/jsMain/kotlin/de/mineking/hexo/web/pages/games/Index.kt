package de.mineking.hexo.web.pages.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.hds.game.FinishedGame
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.Game
import de.mineking.hexo.web.board.Player
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.Anchor
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.ScrollableView
import de.mineking.hexo.web.components.Select
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.formatCompact
import de.mineking.hexo.web.icons.CasualGameIcon
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.icons.StarIcon
import de.mineking.hexo.web.icons.TournamentIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberHdsApiClient
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.url.URL
import kotlin.js.Date

private const val PAGE_SIZE = 10

private enum class RatedFilter(val rated: Boolean?, val queryValue: String, private val label: String) {
    All(null, "all", "All"),
    Rated(true, "rated", "Rated"),
    Casual(false, "casual", "Casual"),
    ;

    override fun toString() = label

    companion object {
        fun fromQuery(value: String?) = entries.find { it.queryValue == value } ?: All
    }
}

@InitRoute
fun initLobbyListPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.FinishedGameList))
}

@Page
@Composable
fun GameHistoryPage(ctx: PageContext) {
    val client = rememberHdsApiClient()
    val initialPage = ctx.route.queryParams["page"]?.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val initialFilter = RatedFilter.fromQuery(ctx.route.queryParams["rated"])

    if (client == null) {
        LoadingState()
    } else {
        GameList(client.finishedGameRepository, initialPage, initialFilter)
    }
}

@Composable
private fun LoadingState() {
    StatusCard {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Loading finished games...")
        }
    }
}

@Composable
private fun GameList(
    finishedGameRepository: FinishedGameRepository,
    initialPage: Int,
    initialFilter: RatedFilter,
) {
    var filter by remember { mutableStateOf(initialFilter) }
    var page by remember { mutableStateOf(initialPage) }
    var games by remember { mutableStateOf(emptyList<FinishedGame>()) }
    var loading by remember { mutableStateOf(true) }

    SyncGameListQueryParameters(page, filter)

    LaunchedEffect(finishedGameRepository, page, filter) {
        loading = true
        games = finishedGameRepository.getFinishedGames(page, PAGE_SIZE, filter.rated)
        loading = false
    }

    ContentCard({
        classes(
            "flex", "max-h-full", "min-h-0", "flex-col", "gap-4", "overflow-hidden", "p-4", "lg:max-h-[calc(100%-3rem)]",
        )
    }) {
        Div({ classes("flex", "shrink-0", "items-center", "justify-between", "gap-3") }) {
            H2({ classes("text-lg", "font-bold", "text-slate-100") }) {
                Text("Finished Games")
            }

            Select(RatedFilter.entries, filter) {
                filter = it
                page = 1
            }
        }

        when {
            loading && games.isEmpty() -> GamesLoadingState()
            games.isEmpty() -> EmptyGameState(filter, page, onPrevious = { page-- })
            else -> {
                Div({ classes("relative", "flex", "min-h-0", "flex-1", "flex-col", "gap-4") }) {
                    ScrollableView({ classes("flex-1", "pr-2") }) {
                        Div({ classes("grid", "gap-3") }) {
                            games.forEach { GameCard(it) }
                        }
                    }

                    Pagination(
                        page = page,
                        hasNextPage = games.size == PAGE_SIZE,
                        onPrevious = { page-- },
                        onNext = { page++ },
                    )

                    if (loading) {
                        Div({
                            classes(
                                "absolute", "inset-0", "z-10", "grid", "place-items-center",
                                "rounded-xl", "bg-slate-900/70", "backdrop-blur-[2px]",
                            )
                            attr("aria-label", "Loading games")
                        }) {
                            LoadingIndicator { classes("size-9") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncGameListQueryParameters(page: Int, filter: RatedFilter) {
    LaunchedEffect(page, filter) {
        val url = URL(window.location.href)
        url.searchParams.set("page", page.toString())
        url.searchParams.set("rated", filter.queryValue)
        window.history.replaceState(null, "", url.toString())
    }
}

@Composable
private fun GamesLoadingState() {
    Div({ classes("grid", "min-h-64", "place-items-center") }) {
        LoadingIndicator { classes("size-9") }
    }
}

@Composable
private fun EmptyGameState(filter: RatedFilter, page: Int, onPrevious: () -> Unit) {
    SubCard({
        classes("grid", "min-h-64", "place-items-center", "border-dashed", "bg-slate-950/40", "p-6", "text-center")
    }) {
        Div({ classes("flex", "flex-col", "items-center", "gap-2") }) {
            H2({ classes("text-base", "font-semibold", "text-slate-200") }) {
                Text(if (page == 1) "No finished games" else "No more games")
            }
            P({ classes("max-w-md", "text-sm", "leading-relaxed", "text-slate-500") }) {
                Text(
                    if (page == 1 && filter != RatedFilter.All) {
                        "No ${filter.toString().lowercase()} games have been recorded yet."
                    } else {
                        "There are no games to show on this page."
                    },
                )
            }
            if (page > 1) {
                ActionButton(label = "Previous page", onClick = onPrevious)
            }
        }
    }
}

@Composable
private fun GameCard(game: FinishedGame) {
    Anchor(AppRoute.FinishedGame(game.id), { classes("block") }) {
        SubCard({
            classes(
                "group", "grid", "gap-3", "p-3", "transition", "hover:border-slate-600/80",
                "hover:bg-slate-700/80", "md:grid-cols-[1fr_auto]", "md:items-center",
            )
        }) {
            Div({ classes("min-w-0") }) {
                Div({ classes("flex", "flex-wrap", "items-center", "gap-2") }) {
                    GameTypeBadge(game)
                    Badge(attrs = {
                        attr("title", game.startedAt.toString())
                    }) {
                        Text(Date(game.startedAt.toEpochMilliseconds().toDouble()).formatMinutePrecision())
                    }
                    Badge {
                        Text(game.result.duration.formatCompact())
                    }
                }

                Div({ classes("mt-2", "flex", "flex-wrap", "items-center", "gap-2") }) {
                    game.players.forEachIndexed { index, player ->
                        if (index > 0) {
                            Span({ classes("text-xs", "font-medium", "text-slate-400") }) {
                                Text("vs")
                            }
                        }
                        Player(player) {
                            classes("font-semibold")
                            if (player == game.result.winner) classes("text-emerald-300!")
                        }
                    }
                }
                P({ classes("mt-1", "truncate", "text-xs", "font-mono", "text-slate-500") }) {
                    Text(game.id.value)
                }
            }

            Div({ classes("flex", "items-center", "gap-3", "text-sm", "text-slate-400") }) {
                Span { Text("${game.moveCount} moves") }
                ChevronRightIcon {
                    classes("size-4", "transition-transform", "group-hover:translate-x-0.5")
                }
            }
        }
    }
}

@Composable
private fun GameTypeBadge(game: Game) {
    Badge(
        when {
            game.tournamentInfo != null -> Color.Sky
            game.options.rated -> Color.Yellow
            else -> Color.Neutral
        },
    ) {
        when {
            game.tournamentInfo != null -> {
                TournamentIcon { classes("size-3.5") }
                Text("Tournament")
            }
            game.options.rated -> {
                StarIcon { classes("size-3.5", "fill-current") }
                Text("Rated")
            }
            else -> {
                CasualGameIcon { classes("size-3.5", "fill-none", "stroke-current") }
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

@Composable
private fun Pagination(
    page: Int,
    hasNextPage: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Div({ classes("flex", "shrink-0", "items-center", "justify-between", "gap-3") }) {
        ActionButton(enabled = page > 1, size = ButtonSize.Medium, attrs = {
            classes("inline-flex", "items-center", "gap-1")
            attr("aria-label", "Previous page")
        }, onClick = onPrevious) {
            ChevronLeftIcon { classes("size-4") }
            Text("Previous")
        }

        Span({ classes("text-sm", "font-semibold", "text-slate-400") }) {
            Text("Page $page")
        }

        ActionButton(enabled = hasNextPage, size = ButtonSize.Medium, attrs = {
            classes("inline-flex", "items-center", "gap-1")
            attr("aria-label", "Next page")
        }, onClick = onNext) {
            Text("Next")
            ChevronRightIcon { classes("size-4") }
        }
    }
}
