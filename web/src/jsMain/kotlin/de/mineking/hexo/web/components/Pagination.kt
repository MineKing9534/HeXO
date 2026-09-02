package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import org.jetbrains.compose.web.attributes.disabled
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
            "flex", "shrink-0", "items-center", "justify-center", "gap-5", "rounded-xl", "border",
            "border-slate-800", "bg-slate-950/60", "p-1",
        )
        attr("aria-label", "Pagination")
        attr("role", "navigation")
    }) {
        PaginationArrowButton("Previous page", currentPage > 1, { onPageChange(currentPage - 1) }) {
            ChevronLeftIcon {
                classes("size-4", "transition-transform", "group-hover:-translate-x-0.5")
            }
        }

        Div({
            classes("flex", "shrink-0", "gap-1")
        }) {
            surroundingPages.forEach { page ->
                PaginationPageButton(page, page == currentPage, onPageChange)
            }
        }

        PaginationArrowButton("Next page", hasNextPage, { onPageChange(currentPage + 1) }) {
            ChevronRightIcon {
                classes("size-4", "transition-transform", "group-hover:translate-x-0.5")
            }
        }
    }
}

@Composable
private fun PaginationArrowButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Button({
        classes(
            "group", "grid", "size-8", "place-items-center", "rounded-lg", "border-0", "bg-transparent",
            "transition", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/40",
        )
        if (enabled) {
            classes("cursor-pointer", "text-slate-400", "hover:bg-slate-800", "hover:text-slate-100")
        } else {
            classes("text-slate-700")
            disabled()
        }
        attr("aria-label", label)
        onClick { onClick() }
    }) {
        icon()
    }
}

@Composable
private fun PaginationPageButton(page: Int, current: Boolean, onPageChange: (Int) -> Unit) {
    if (current) {
        Span({
            classes(
                "grid", "size-8", "place-items-center", "rounded-lg", "bg-emerald-500/15", "text-sm", "font-bold",
                "tabular-nums", "text-emerald-200", "ring-1", "ring-emerald-400/30",
            )
            attr("aria-current", "page")
            attr("aria-label", "Page $page, current page")
        }) {
            Text("$page")
        }
    } else {
        Button({
            classes(
                "grid", "size-8", "cursor-pointer", "place-items-center", "rounded-lg", "border-0",
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
