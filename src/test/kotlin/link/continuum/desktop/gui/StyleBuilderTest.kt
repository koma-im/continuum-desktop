package link.continuum.desktop.gui

import kotlin.test.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.exp

internal class StyleBuilderTest {
    @Test
    fun test0() {
        val s = StyleBuilder().apply {
            val size = 2.em
            minHeight = size
            minWidth = size
            maxHeight = size
            maxWidth = size
            prefHeight = size
            prefWidth = size
        }.toString()
        val expect = "-fx-min-height:2em;-fx-min-width:2em;-fx-max-height:2em;-fx-max-width:2em;-fx-pref-height:2em;-fx-pref-width:2em;"
        assertEquals(expect, s)
    }
}