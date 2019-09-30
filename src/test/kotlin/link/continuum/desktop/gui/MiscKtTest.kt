package link.continuum.desktop.gui

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Parent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class MiscKtTest {
    @Test
    fun testPropertyDelegate() {
        class Testing {
            val property = SimpleObjectProperty<Color> ()
            var color by prop(property)
        }
        val t0 = Testing()
        assertEquals(null, t0.color)
        assertEquals(null, t0.property.get())
        t0.color = Color.AQUAMARINE
        assertEquals(Color.AQUAMARINE, t0.property.get())
        assertEquals(Color.AQUAMARINE, t0.color)
        t0.property.set(Color.CORNFLOWERBLUE)
        assertEquals(Color.CORNFLOWERBLUE, t0.property.get())
        assertEquals(Color.CORNFLOWERBLUE, t0.color)
    }
    class ParentBoundsException: Exception()
    class BrokenParent(): Parent() {
        override fun updateBounds() {
            throw ParentBoundsException()
        }
    }
    class OutOfBoundsParent(): Parent() {
        var updateBoundsCount = 0
        override fun updateBounds() {
            updateBoundsCount += 1
            throw IndexOutOfBoundsException("testing-out-of-bounds")
        }
    }
    @Test
    fun callUpdateCachedBounds() {
        val h = StackPane()
        h.children.add(BrokenParent())
        assertThrows<ParentBoundsException> { h.callUpdateBoundsReflectively() }
        val p = BrokenParent()
        val h1 = HBox().apply {
            children.add(p)
        }
        assertThrows<ParentBoundsException> { h1.callUpdateBoundsReflectively() }
        
        val p2 = OutOfBoundsParent()
        val h2 = HBox().apply {
            children.addAll(Text(), Text(), p2)
        }
        assertEquals(0, p2.updateBoundsCount)
        h2.callUpdateBoundsReflectively()
        assertEquals(2, p2.updateBoundsCount)
        assertEquals(1, h2.brokenChildren.size)
        assertSame(2, h2.brokenChildren[0].first)
        assertSame(p2, h2.brokenChildren[0].second)

        val p3 = OutOfBoundsParent()
        val s = StackPane().apply {
            children.add(p3)
        }
        val h3 = HBox().apply {
            children.add(s)
        }
        assertEquals(0, p3.updateBoundsCount)
        h3.callUpdateBoundsReflectively()
        assertEquals(3, p3.updateBoundsCount)
        assertEquals(2, h3.brokenChildren.size)
        assertSame(s, h3.brokenChildren[0].second)
        assertSame(p3, h3.brokenChildren[1].second)
        val su = StackPane(h3)
        h3.callUpdateBoundsReflectively()
    }
}