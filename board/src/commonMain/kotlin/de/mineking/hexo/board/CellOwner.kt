package de.mineking.hexo.board

enum class CellOwner(val symbol: String) {
    X("x") {
        override val other get() = O
    },
    O("o") {
        override val other get() = X
    },
    ;

    abstract val other: CellOwner
}
