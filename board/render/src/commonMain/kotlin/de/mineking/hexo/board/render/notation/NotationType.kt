package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.render.BoardRenderer

enum class NotationType(val renderer: BoardRenderer<Unit, String>) {
    CompactRectilinear(RectilinearNotationBoardRenderer.withType(RectilinearNotationType.Compact)),
    MultilineRectilinear(RectilinearNotationBoardRenderer.withType(RectilinearNotationType.Multiline)),
    HTTTX(HTTTXNotationBoardRenderer),
    Tyto(TytoNotationBoardRenderer),
}
