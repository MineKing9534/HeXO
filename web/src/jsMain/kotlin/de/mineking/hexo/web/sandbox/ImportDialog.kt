package de.mineking.hexo.web.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.hds.asBoard
import de.mineking.hexo.hds.formation.FormationId
import de.mineking.hexo.hds.formation.FormationRepository
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.TextInput
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg

@Composable
fun ImportDialog(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    onClose: () -> Unit,
    onConfirm: (Board) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val valid by derivedStateOf { url.isNotBlank() && !error }

    Dialog(
        title = "Import Position",
        onClose = onClose,
        actionRow = {
            ConfirmButton(
                formationRepository = formationRepository,
                finishedGameRepository = finishedGameRepository,
                url = url,
                valid = valid,
                onErrorUpdate = { error = it },
                onConfirm = onConfirm,
            )
        },
    ) {
        Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-300") }) {
            Text("URL")
        }
        Div({ classes("text-xs", "text-slate-500") }) {
            Text("Game or Sandbox Position Link")
        }

        UrlInput(
            url = url,
            onUrlUpdate = {
                url = it
                error = false
            },
            valid = valid,
        )

        if (error) {
            Div({ classes("min-h-5", "text-sm", "leading-relaxed", "text-rose-400") }) {
                Span({ classes("font-bold", "uppercase") }) {
                    Text("Error: ")
                }
                Text("Position or game not found")
            }
        }
    }
}

@Composable
private fun UrlInput(url: String, onUrlUpdate: (String) -> Unit, valid: Boolean) {
    TextInput(
        value = url,
        type = InputType.Url,
        placeholder = "https://hexo.did.science/sandbox/2mdyn02",
        valid = valid,
        attrs = { classes("text-ellipsis") },
        onValueInput = onUrlUpdate,
    )
    Div({ classes("text-xs", "text-slate-500") }) {
        Span({ classes("font-bold", "uppercase") }) {
            Text("Hint: ")
        }
        Text("You can add ")
        Badge(Color.Sky, {
            classes("mx-1", "rounded-md", "px-1", "py-0.5", "font-mono", "font-semibold")
        }) {
            Text("?move=...")
        }
        Text(" to a game url to import the position at a specific move!")
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun ConfirmButton(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    url: String,
    valid: Boolean,
    onErrorUpdate: (Boolean) -> Unit,
    onConfirm: (Board) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    Button({
        if (!valid) disabled()

        classes(
            "rounded-lg", "border", "px-4", "py-2", "transition", "text-sm", "font-medium", "h-10", "w-24",
            "flex", "items-center", "justify-center", "gap-1.5",
        )

        if (loading || !valid) {
            classes("border-slate-500/40", "bg-slate-500/15", "text-slate-300")
        } else {
            classes(
                "border-emerald-500/40", "bg-emerald-500/15", "text-emerald-300",
                "hover:bg-emerald-500/25", "hover:text-emerald-100", "cursor-pointer",
            )
        }

        onClick {
            loading = true
            onErrorUpdate(false)
            coroutineScope.launch {
                try {
                    val board = url.urlToBoard(formationRepository, finishedGameRepository)
                    if (board == null) {
                        onErrorUpdate(true)
                        return@launch
                    }

                    onConfirm(board)
                } finally {
                    loading = false
                }
            }
        }
    }) {
        if (loading) {
            LoadingIndicator { classes("size-6") }
        } else {
            Svg("0 0 24 24", {
                attr("fill", "none")
                attr("stroke", "currentColor")
                attr("stroke-width", "2")
                attr("stroke-linecap", "round")
                attr("stroke-linejoin", "round")
                classes("size-5", "shrink-0")
            }) {
                Path("M12 15V3")
                Path("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4")
                Path("m7 10 5 5 5-5")
            }
            Text("Import")
        }
    }
}

private val FORMATION_URL = """^https?://.*?/sandbox/(.*)$""".toRegex()
private val GAME_URL = """^https?://.*?/games/(.*?)(?:\?move=(\d+))?$""".toRegex()
private suspend fun String.urlToBoard(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
): Board? {
    val formationMatch = FORMATION_URL.matchEntire(this)
    val gameMatch = GAME_URL.matchEntire(this)

    return when {
        formationMatch != null -> formationRepository.getFormation(FormationId(formationMatch.groupValues[1]))?.asBoard()
        gameMatch != null -> {
            val move = gameMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toInt()
            finishedGameRepository.getGame(GameId(gameMatch.groupValues[1]))?.asBoard(move ?: Int.MAX_VALUE)
        }
        else -> null
    }
}
