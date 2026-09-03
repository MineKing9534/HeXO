@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement

enum class SubCardVariant {
    Muted,
    Inset,
    Empty,
    Deep,
    Highlighted,
}

@Composable
fun Card(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classes(
            "rounded-2xl", "border", "border-slate-800", "bg-slate-800/40",
            "shadow-xl", "shadow-black/10",
        )
        attrs?.invoke(this)
    }, content)
}

@Composable
fun ContentCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({ classes("lg:h-12") })
    Card({
        classes("max-w-5xl", "w-full", "mx-auto")
        attrs?.invoke(this)
    }, content)
}

@Composable
fun CardHeader(
    title: String,
    supportingText: String? = null,
    truncateSupportingText: Boolean = true,
    iconAttrs: AttrBuilderContext<HTMLSpanElement>? = null,
    icon: @Composable () -> Unit,
) {
    Div({ classes("flex", "min-w-0", "items-center", "gap-3") }) {
        Span({
            classes("grid", "size-9", "shrink-0", "place-items-center", "rounded-lg", "border")
            iconAttrs?.invoke(this)
        }) {
            icon()
        }
        Div({ classes("min-w-0") }) {
            H2({ classes("text-lg", "font-bold", "leading-tight", "text-slate-100", "uppercase") }) {
                Text(title)
            }
            if (supportingText != null) {
                P({
                    classes("mt-0.5", "text-xs", "text-slate-500")
                    if (truncateSupportingText) classes("truncate") else classes("leading-relaxed")
                }) {
                    Text(supportingText)
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    compact: Boolean = false,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({ classes("mx-6", "flex-1", "h-full", "flex", "flex-col") }) {
        ContentCard({
            classes("grid", "place-items-center", "my-auto")
            if (compact) {
                classes("min-h-64", "p-6", "lg:max-w-3xl")
            } else {
                classes("min-h-48", "p-8", "lg:max-w-3xl")
            }
            attrs?.invoke(this)
        }) {
            Section({ classes("grid", "place-items-center", "gap-4", "w-full") }) {
                content()
            }
        }
    }
}

@Composable
fun SubCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    variant: SubCardVariant = SubCardVariant.Muted,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classes("rounded-xl", "border", "shadow-md", "shadow-slate-700/10")
        when (variant) {
            SubCardVariant.Muted -> classes("border-slate-700/60", "bg-slate-900/50")
            SubCardVariant.Inset -> classes("border-slate-700/70", "bg-slate-950/35")
            SubCardVariant.Empty -> classes(
                "grid", "min-h-64", "place-items-center", "border-dashed", "border-slate-600/40",
                "bg-slate-950/40", "p-6", "text-center",
            )
            SubCardVariant.Deep -> classes("border-slate-700", "bg-slate-950/25")
            SubCardVariant.Highlighted -> classes("border-emerald-400/80", "bg-slate-700/25")
        }
        attrs?.invoke(this)
    }, content)
}
