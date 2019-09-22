package link.continuum.desktop.gui

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.paint.Color
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

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
}