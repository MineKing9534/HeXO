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
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.TextInput
import de.mineking.hexo.web.icons.DownloadIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
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
        onValueChange = onUrlUpdate,
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

    val parser = remember { RemoteBoardParser(formationRepository, finishedGameRepository) }

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
                    onConfirm(parser.parse(url))
                } catch (_: HexoNotationException) {
                    onErrorUpdate(true)
                } finally {
                    loading = false
                }
            }
        }
    }) {
        if (loading) {
            LoadingIndicator { classes("size-6") }
        } else {
            DownloadIcon {
                classes("size-5", "shrink-0")
            }
            Text("Import")
        }
    }
}
