plugins {
    id("latex")
    id("kotlin-multiplatform")
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspLatex", projects.board.latex.processor)
}

ksp {
    arg(
        "themes",
        listOf(
            "de.mineking.hexo.board.render.image.theme.HDSTheme",
            "de.mineking.hexo.board.render.image.theme.HTTTXTheme",
            "de.mineking.hexo.board.render.image.theme.TytoTheme",
        ).joinToString()
    )
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
    requirePackage("graphicx")
    requirePackage("pdfrender")

    preamble("""
        \usetikzlibrary{calc,fadings}
    
        \newcommand{\hexolabelfont}{\Huge\bfseries}
        \newlength{\hexolabelmaskwidth}
        \setlength{\hexolabelmaskwidth}{4pt}
        \newcommand{\hexolabelwithoutcolor}[1]{{\renewcommand{\color}[2][]{}#1}}
        
        \tikzfading[name=fade l,left color=transparent!100,right color=transparent!0]
        \tikzfading[name=fade r,right color=transparent!100,left color=transparent!0]
        \tikzfading[name=fade d,bottom color=transparent!100,top color=transparent!0]
        \tikzfading[name=fade u,top color=transparent!100,bottom color=transparent!0]

        \newcommand\framenode[2][15pt]{
            \fill[white,path fading=fade u] (#2.south west) rectangle ($(#2.south east)+(0, #1)$);
            \fill[white,path fading=fade d] (#2.north west) rectangle ($(#2.north east)+(0,-#1)$);
            \fill[white,path fading=fade l] (#2.south east) rectangle ($(#2.north east)+(-#1,0)$);
            \fill[white,path fading=fade r] (#2.south west) rectangle ($(#2.north west)+( #1,0)$);
        }
        
        \newcommand{\fadeedges}[2][15pt]{
            \begin{tikzpicture}%
                \node[anchor=south west,inner sep=0] (content) at (0,0) {#2};%
                \framenode[#1]{content}%
            \end{tikzpicture}%
        }
    """.trimIndent())
}
