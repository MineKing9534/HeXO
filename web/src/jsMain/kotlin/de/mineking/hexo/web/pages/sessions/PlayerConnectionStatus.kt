package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun PlayerConnectionStatus(
    status: SessionPlayerConnectionStatus,
    disconnectedAsWaiting: Boolean = false,
) {
    val waiting = disconnectedAsWaiting && status == SessionPlayerConnectionStatus.Disconnected

    Span({ classes("inline-flex", "items-center", "gap-1.5", "whitespace-nowrap") }) {
        Span({ classes("size-1.5", "rounded-full", if (waiting) "bg-amber-400" else status.dotColor) })
        Span({ classes(if (waiting) "text-amber-400" else status.textColor) }) {
            Text(if (waiting) "Waiting" else status.label)
        }
    }
}

internal val SessionPlayerConnectionStatus.label get() = when (this) {
    SessionPlayerConnectionStatus.Connected -> "Connected"
    SessionPlayerConnectionStatus.Orphaned -> "Reconnecting"
    SessionPlayerConnectionStatus.Disconnected -> "Disconnected"
}

private val SessionPlayerConnectionStatus.dotColor get() = when (this) {
    SessionPlayerConnectionStatus.Connected -> "bg-emerald-400"
    SessionPlayerConnectionStatus.Orphaned -> "bg-amber-400"
    SessionPlayerConnectionStatus.Disconnected -> "bg-rose-400"
}

private val SessionPlayerConnectionStatus.textColor get() = when (this) {
    SessionPlayerConnectionStatus.Connected -> "text-emerald-400"
    SessionPlayerConnectionStatus.Orphaned -> "text-amber-400"
    SessionPlayerConnectionStatus.Disconnected -> "text-rose-400"
}
