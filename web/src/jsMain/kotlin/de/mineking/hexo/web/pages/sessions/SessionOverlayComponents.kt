package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.isGuest
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.session.SessionPlayer
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun SessionSectionLabel(text: String) {
    Span({ classes("text-xs", "font-semibold", "tracking-wide", "text-slate-500", "uppercase") }) {
        Text(text)
    }
}

@Composable
internal fun SessionMetric(
    label: String,
    valueColor: String = "text-slate-100",
    content: @Composable () -> Unit,
) {
    Div({ classes("flex", "items-center", "justify-between", "gap-4", "px-4", "py-3.5", "sm:block", "sm:text-center") }) {
        SessionSectionLabel(label)
        P({ classes("font-bold", "tabular-nums", valueColor, "sm:mt-1", "sm:text-lg") }) {
            content()
        }
    }
}

@Composable
internal fun SessionPlayerMeta(
    player: SessionPlayer,
    disconnectedAsWaiting: Boolean = false,
    eloAdjustment: Int? = null,
) {
    Div({ classes("mt-0.5", "flex", "items-center", "justify-center", "gap-1.5", "text-xs") }) {
        if (!player.isGuest()) {
            PlayerElo(player, eloAdjustment)
            Span({ classes("text-slate-700") }) { Text("·") }
        }
        PlayerConnectionStatus(player.connectionStatus, disconnectedAsWaiting)
    }
}

@Composable
internal fun PlayerElo(player: Player, adjustment: Int? = null) {
    Span({ classes("text-slate-400") }) {
        Text("ELO ${player.elo}")
        if (adjustment != null) {
            Span({ classes("ml-1", "font-semibold", adjustment.color) }) {
                Text(if (adjustment > 0) "+$adjustment" else "$adjustment")
            }
        }
    }
}

private val Int.color get() = when {
    this > 0 -> "text-emerald-300"
    this < 0 -> "text-rose-300"
    else -> "text-slate-300"
}

@Composable
internal fun SessionPlayerIcon(player: Player?) {
    Div({
        classes("grid", "size-11", "place-items-center", "overflow-hidden", "rounded-full", "border", "text-sm", "font-bold")
        playerIconColor(player)
    }) {
        var profile by remember { mutableStateOf<Profile?>(null) }
        LaunchedEffect(player) { profile = player?.profile?.retrieve() }

        profile?.image?.let { Img(it) } ?: Text(player?.displayName?.take(1)?.uppercase() ?: "?")
    }
}

private fun AttrsScope<*>.playerIconColor(player: Player?) {
    when {
        player == null -> classes("border-dashed", "border-slate-600", "text-slate-600")
        player !is SessionPlayer -> classes("border-emerald-400/35", "bg-emerald-500/15", "text-emerald-200")
        player.connectionStatus == SessionPlayerConnectionStatus.Connected ->
            classes("border-emerald-400/35", "bg-emerald-500/15", "text-emerald-200")
        player.connectionStatus == SessionPlayerConnectionStatus.Orphaned ->
            classes("border-amber-400/30", "bg-amber-500/10", "text-amber-200")
        else -> classes("border-slate-600", "bg-slate-800/60", "text-slate-400")
    }
}
