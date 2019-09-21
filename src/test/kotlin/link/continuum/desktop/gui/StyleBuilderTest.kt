package link.continuum.desktop.gui

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

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
        assertEquals("-fx-pref-width:2em;-fx-pref-height:2em;-fx-min-width:2em;-fx-min-height:2em;-fx-max-width:2em;-fx-max-height:2em;", s)
    }
}