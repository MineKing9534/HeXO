package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.render.image.svg.SvgBoardRenderer
import de.mineking.hexo.board.render.outputUtf8Bytes

class SvgRenderTest : AbstractImageRendererTest("svg", SvgBoardRenderer.Default.outputUtf8Bytes())
