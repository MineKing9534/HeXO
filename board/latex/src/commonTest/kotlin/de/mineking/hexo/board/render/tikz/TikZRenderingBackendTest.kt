package de.mineking.hexo.board.render.tikz

import de.mineking.hexo.board.render.image.BoundingBox
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TikZRenderingBackendTest {
    @Test
    fun `text fading masks lines only`() {
        val backend = TikZRenderingBackend()
        backend.drawPolygon(
            Polygon(listOf(Point(-5, -5), Point(5, -5), Point(5, 5), Point(-5, 5))),
            RED,
            null,
            0f,
        )
        backend.drawLine(Point.Zero, Point(10, 10), Stroke(BLUE, 2f), Stroke(GREEN, 1f))
        backend.drawString(Point.Zero, "label", 100.0, 12f, FontType.MonospaceRegular, BLUE)

        val output = backend.render(BOUNDS, Color.Transparent)
        val picture = output.substringAfterLast("\\begin{tikzpicture}")
        val polygon = picture.lineSequence().single { "fill={rgb,255:red,255;green,0;blue,0}" in it }
        val lines = picture.lineSequence().filter { "(0, 0) -- (10, 10)" in it }.toList()
        val label = picture.lineSequence().single { "{label};" in it }

        assertContains(output, "\\pgfdeclarefading{hexotextmask\\the\\hexotextmaskid}")
        assertFalse("path fading=" in polygon)
        assertEquals(2, lines.size)
        assertTrue(lines.all { "path fading=hexotextmask\\the\\hexotextmaskid" in it })
        assertFalse("path fading=" in label)
    }

    @Test
    fun `line without text does not create a fading`() {
        val backend = TikZRenderingBackend()
        backend.drawLine(Point.Zero, Point(10, 10), Stroke(BLUE, 2f))

        val output = backend.render(BOUNDS, Color.Transparent)

        assertFalse("\\pgfdeclarefading" in output)
        assertFalse("path fading=" in output)
    }

    @Test
    fun `text without a line does not create a fading`() {
        val backend = TikZRenderingBackend(rawLabels = false)
        backend.drawString(Point.Zero, "label", 100.0, 12f, FontType.MonospaceRegular, BLUE)

        val output = backend.render(BOUNDS, Color.Transparent)

        assertFalse("\\pgfdeclarefading" in output)
        assertEquals(1, output.lineSequence().count { "{label};" in it })
    }

    @Test
    fun `zero length line uses the text fading`() {
        val backend = TikZRenderingBackend(rawLabels = false)
        backend.drawLine(Point.Zero, Point.Zero, Stroke(BLUE, 2f))
        backend.drawString(Point.Zero, "label", 100.0, 12f, FontType.MonospaceRegular, BLUE)

        val picture = backend.render(BOUNDS, Color.Transparent).substringAfterLast("\\begin{tikzpicture}")
        val line = picture.lineSequence().single { "circle[radius=0.75bp]" in it }

        assertTrue("path fading=hexotextmask\\the\\hexotextmaskid" in line)
    }

    private companion object {
        val BOUNDS = BoundingBox(-10.0, 10.0, -10.0, 10.0)
        val RED = Color.rgb(0xff0000)
        val GREEN = Color.rgb(0x00ff00)
        val BLUE = Color.rgb(0x0000ff)
    }
}
