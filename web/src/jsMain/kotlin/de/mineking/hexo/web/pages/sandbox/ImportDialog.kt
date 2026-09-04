package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.RemoteBoardParser
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.components.TextInput
import de.mineking.hexo.web.icons.DownloadIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

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
        supportingText = "Load a board from a shared position or finished game.",
        onClose = onClose,
        actionRow = {
            ImportButton(
                formationRepository = formationRepository,
                finishedGameRepository = finishedGameRepository,
                url = url,
                valid = valid,
                onErrorUpdate = { error = it },
                onConfirm = onConfirm,
            )
        },
    ) {
        Div({ classes("grid", "gap-3", "rounded-xl", "border", "border-slate-800", "bg-slate-950/35", "p-3") }) {
            Div({ classes("grid", "gap-0.5") }) {
                Div({ classes("text-sm", "font-semibold", "text-slate-200") }) { Text("Position URL") }
                Div({ classes("text-xs", "text-slate-500") }) { Text("Paste a game or sandbox link") }
            }
            UrlInput(
                url = url,
                onUrlUpdate = {
                    url = it
                    error = false
                },
                valid = valid,
            )
        }

        if (error) {
            Div({
                classes(
                    "rounded-lg", "border", "border-rose-400/25", "bg-rose-400/8", "px-3", "py-2",
                    "text-sm", "text-rose-300",
                )
            }) {
                Text("The position or game could not be found.")
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
        onValueChange = onUrlUpdate,
    )
    Div({ classes("text-xs", "leading-relaxed", "text-slate-500") }) {
        Text("To import a specific game state, append ")
        Span({ classes("rounded", "bg-slate-800", "px-1", "py-0.5", "font-mono", "text-sky-300") }) {
            Text("?move=…")
        }
        Text(" to the game URL.")
    }
}

@Composable
private fun ImportButton(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    url: String,
    valid: Boolean,
    onErrorUpdate: (Boolean) -> Unit,
    onConfirm: (Board) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    val parser = remember { RemoteBoardParser(formationRepository, finishedGameRepository) }

    ActionButton(
        enabled = valid && !loading,
        size = ButtonSize.Medium,
        color = Color.Sky,
        attrs = { classes("inline-flex", "items-center", "justify-center", "gap-2", "px-5!") },
        onClick = {
            loading = true
            onErrorUpdate(false)
            coroutineScope.launch {
                try {
                    onConfirm(parser.parse(url))
                } catch (_: HexoNotationException) {
                    onErrorUpdate(true)
                } finally {
                    loading = false
                }
            }
        },
    ) {
        if (loading) {
            LoadingIndicator(LoadingIndicatorSize.Medium)
        } else {
            DownloadIcon {
                classes("size-4", "shrink-0")
            }
            Text("Import")
        }
    }
}
