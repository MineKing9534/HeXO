package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LoadingCard(label: String) {
    StatusCard(attrs = {
        classes("min-h-0!", "overflow-hidden", "p-5!", "sm:p-6!")
        attr("role", "status")
        attr("aria-live", "polite")
    }) {
        Div({ classes("w-full") }) {
            CardHeader(
                title = label.removeSuffix("..."),
                supportingText = "Please wait while the latest data is loaded. This view will update automatically.",
                truncateSupportingText = false,
                iconAttrs = {
                    classes("border-emerald-400/20", "bg-emerald-400/10", "text-emerald-300")
                },
            ) {
                LoadingIndicator(LoadingIndicatorSize.Tiny)
            }
            SubCard({ classes("mt-5", "p-4", "sm:p-5") }, SubCardVariant.Inset) {
                Span({ classes("text-xs", "font-bold", "tracking-wider", "text-slate-400", "uppercase") }) {
                    Text("Loading in progress")
                }
                P({ classes("mt-2", "max-w-2xl", "text-sm", "leading-relaxed", "text-slate-300") }) {
                    Text("No action is required. You can keep this page open while the requested information is retrieved.")
                }
            }
        }
    }
}
