package link.continuum.desktop.gui

import javafx.scene.text.Text
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class CatchingGroupTest {
    @Test
    fun test1() {
        val gq = CatchingGroup()
        val n = Text("x")
        gq.dirtyChildren = arrayListOf(n)
        gq.dirtyChildrenCount = 3
        assertEquals(1, gq.dirtyChildren!!.size)
        assertSame(n, gq.dirtyChildren!![0])
        assertEquals(3, gq.dirtyChildrenCount)
        ParentReflection.clearDirtyChildren(gq)
        assertEquals(0, gq.dirtyChildren!!.size)
        assertEquals(0, gq.dirtyChildrenCount)
    }
}