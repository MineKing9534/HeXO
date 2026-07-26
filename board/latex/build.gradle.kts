plugins {
    id("latex")
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.latexMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.board.parse)
            implementation(projects.board.render)
        }
    }
}

latexPackage {
    packageName.set("hexo")
    outputFileName.set("hexo.sty")

    requirePackage("tikz")
    requirePackage("pdfrender")
    preamble("""
        \newcommand{\hexolabelfont}{\Huge\bfseries}
        \newlength{\hexolabelmaskwidth}
        \setlength{\hexolabelmaskwidth}{4pt}
        \newcommand{\hexolabelwithoutcolor}[1]{{\renewcommand{\color}[2][]{}#1}}
    """.trimIndent())
}
