package de.mineking.hexo.web.components

import org.jetbrains.compose.web.attributes.AttrsScope

enum class Color {
    Neutral,
    Emerald,
    Sky,
    Yellow,
    Rose,
}

internal fun AttrsScope<*>.colorClasses(color: Color) {
    when (color) {
        Color.Neutral -> classes("border-slate-700", "bg-slate-950/60", "text-slate-300")
        Color.Emerald -> classes("border-emerald-500/40", "bg-emerald-500/15", "text-emerald-300")
        Color.Sky -> classes("border-sky-500/40", "bg-sky-500/15", "text-sky-300")
        Color.Yellow -> classes("border-yellow-400/40", "bg-yellow-400/15", "text-yellow-200")
        Color.Rose -> classes("border-rose-400/40", "bg-rose-500/15", "text-rose-400")
    }
}

internal fun AttrsScope<*>.colorButtonClasses(color: Color, enabled: Boolean) {
    if (!enabled) {
        classes("border-slate-700", "bg-slate-800", "text-slate-300")
        return
    }

    when (color) {
        Color.Neutral -> classes(
            "border-slate-600/80", "bg-slate-950", "text-slate-300", "disabled:text-slate-400",
            "not-disabled:hover:bg-slate-700", "not-disabled:hover:text-slate-100",
        )

        Color.Emerald -> classes(
            "border-emerald-500/40", "bg-emerald-500/15", "text-emerald-300",
            "hover:bg-emerald-500/25", "hover:text-emerald-100",
        )

        Color.Sky -> classes("border-sky-500/40", "bg-sky-500/15", "text-sky-300", "hover:bg-sky-500/25")
        Color.Yellow -> classes("border-yellow-400/40", "bg-yellow-400/15", "text-yellow-200", "hover:bg-yellow-400/25")
        Color.Rose -> classes("border-rose-400/40", "bg-rose-500/15", "text-rose-400", "hover:bg-rose-500/25")
    }
}
