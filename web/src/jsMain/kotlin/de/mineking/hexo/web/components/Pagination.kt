package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun Pagination(
    currentPage: Int,
    hasNextPage: Boolean,
    surroundingPageCount: Int = 2,
    onPageChange: (Int) -> Unit,
) {
    val surroundingPages = surroundingPages(currentPage, surroundingPageCount, hasNextPage)

    Div({
        classes(
            "flex", "shrink-0", "items-center", "justify-between", "gap-2", "rounded-xl", "border",
            "border-slate-800/80", "bg-slate-950/45", "p-1.5",
        )
        attr("aria-label", "Pagination")
        attr("role", "navigation")
    }) {
        ActionButton(enabled = currentPage > 1, size = ButtonSize.Medium, attrs = {
            classes("group", "inline-flex", "items-center", "gap-1.5", "disabled:opacity-40")
            attr("aria-label", "Previous page")
        }, onClick = { onPageChange(currentPage - 1) }) {
            ChevronLeftIcon {
                classes("size-4", "transition-transform", "group-hover:-translate-x-0.5")
            }
            Span({ classes("hidden", "sm:inline") }) { Text("Previous") }
        }

        Div({
            classes(
                "flex", "items-center", "justify-center", "gap-0.5", "rounded-lg", "border",
                "border-slate-800", "bg-slate-950/70", "p-0.5",
            )
        }) {
            surroundingPages.forEach { page ->
                PaginationPageButton(page, page == currentPage, onPageChange)
            }
        }

        ActionButton(enabled = hasNextPage, size = ButtonSize.Medium, attrs = {
            classes("group", "inline-flex", "items-center", "gap-1.5", "disabled:opacity-40")
            attr("aria-label", "Next page")
        }, onClick = { onPageChange(currentPage + 1) }) {
            Span({ classes("hidden", "sm:inline") }) { Text("Next") }
            ChevronRightIcon {
                classes("size-4", "transition-transform", "group-hover:translate-x-0.5")
            }
        }
    }
}

@Composable
private fun PaginationPageButton(page: Int, current: Boolean, onPageChange: (Int) -> Unit) {
    if (current) {
        Span({
            classes(
                "grid", "size-8", "place-items-center", "rounded-md", "bg-slate-700", "text-sm", "font-bold",
                "tabular-nums", "text-white", "shadow-sm", "shadow-black/30",
            )
            attr("aria-current", "page")
            attr("aria-label", "Page $page, current page")
        }) {
            Text("$page")
        }
    } else {
        Button({
            classes(
                "grid", "size-8", "cursor-pointer", "place-items-center", "rounded-md", "border-0",
                "bg-transparent", "text-sm", "font-medium", "tabular-nums", "text-slate-500", "transition",
                "hover:bg-slate-800", "hover:text-slate-200", "focus:outline-none", "focus-visible:ring-2",
                "focus-visible:ring-slate-500/60",
            )
            attr("aria-label", "Go to page $page")
            onClick { onPageChange(page) }
        }) {
            Text("$page")
        }
    }
}

private fun surroundingPages(currentPage: Int, surroundingPageCount: Int, hasNextPage: Boolean): IntRange {
    val firstPage = maxOf(1, currentPage - surroundingPageCount)
    val lastPage = if (hasNextPage) currentPage + surroundingPageCount else currentPage
    return firstPage..lastPage
}
