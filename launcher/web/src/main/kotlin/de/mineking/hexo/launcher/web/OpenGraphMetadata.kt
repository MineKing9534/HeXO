package de.mineking.hexo.launcher.web

import kotlinx.html.TagConsumer
import kotlinx.html.meta
import kotlinx.html.stream.appendHTML

data class OpenGraphMetadata(
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
) {
    companion object {
        val Default = OpenGraphMetadata(
            title = "HeXO · Play, Analyse, Connect",
            description = "Connect six consecutive tiles in a row on a hexagonal grid to win. Host or watch a live session or review past games!",
        )
    }

    fun render(url: String) = buildString {
        with(appendHTML(prettyPrint = true)) {
            openGraphMeta("og:type", "website")
            openGraphMeta("og:site_name", "HeXO · Play, Analyse, Connect")
            openGraphMeta("og:title", title)
            meta(name = "theme-color", content = "#38bdf8")
            description?.let {
                meta(name = "description", content = it)
                openGraphMeta("og:description", it)
            }
            imageUrl?.let {
                openGraphMeta("og:image", it)
                meta(name = "twitter:card", content = "summary_large_image")
            }
            openGraphMeta("og:url", url)
        }
    }
}

private fun TagConsumer<*>.openGraphMeta(property: String, content: String) {
    meta {
        attributes["property"] = property
        attributes["content"] = content
    }
}

internal fun String.injectOpenGraph(url: String, metaData: OpenGraphMetadata) = replace(OPEN_GRAPH_PLACEHOLDER, metaData.render(url))

private const val OPEN_GRAPH_PLACEHOLDER = "<!-- HEXO_OPEN_GRAPH -->"
