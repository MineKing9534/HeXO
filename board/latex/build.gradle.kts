import de.mineking.kotlinlatex.gradle.latexMain

plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")
    id("publish")

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
            implementation(libs.kotlin.coroutines.core)
        }
    }
}

latexPackage {
    packageName.set("hexo")
    outputFileName.set("hexo.sty")

    requirePackage("tikz")
    requirePackage("graphicx")
    requirePackage("currfile")

    preamble("""
        \usetikzlibrary{calc,external,fadings}

        \newif\ifhexoexternalization
        \hexoexternalizationfalse
        \NewDocumentCommand{\hexoexternalize}{O{} O{}}{%
            \def\hexocacheprefix{#1}%
            \def\hexoexternalsource{#2}%
            \if\relax\detokenize{#2}\relax
                \IfFileExists{\jobname.tex}{%
                    \edef\hexoexternalsource{\jobname}%
                }{%
                    \IfFileExists{main.tex}{%
                        \def\hexoexternalsource{main}%
                    }{%
                        \edef\hexoexternalsource{\currfilebase}%
                    }%
                }%
            \fi
            \edef\hexoexternalrealjob{\jobname}%
            \edef\hexoexternalsystemcall{%
                lualatex \noexpand\tikzexternalcheckshellescape -halt-on-error -interaction=batchmode
                -jobname "\noexpand\image"
                "\string\def\string\tikzexternalrealjob{\hexoexternalrealjob}\string\input{\hexoexternalsource}"%
            }%
            \tikzexternalize[prefix=#1]%
            \tikzset{external/system call/.expand once=\hexoexternalsystemcall}%
            \tikzexternaldisable
            \hexoexternalizationtrue
        }
        \newcommand{\hexocacheversion}{1}
        \newcommand{\hexopreparepicture}[1]{%
            \ifhexoexternalization
                \tikzsetnextfilename{hexo-#1}%
                \IfFileExists{\hexocacheprefix hexo-#1.pdf}{%
                    \tikzset{external/mode=only graphics}%
                }{%
                    \tikzset{external/mode=convert with system call}%
                }%
                \tikzexternalenable
            \fi
        }
        \newcommand{\hexofinishpicture}{%
            \ifhexoexternalization
                \tikzexternaldisable
            \fi
        }
    
        \newcommand{\hexolabelfont}{\Huge\bfseries}
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
