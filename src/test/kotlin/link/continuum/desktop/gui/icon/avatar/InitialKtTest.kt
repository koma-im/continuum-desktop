package link.continuum.desktop.gui.icon.avatar

import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

internal class InitialKtTest {
    @Test
    fun test() {
        val c1 = extractKeyChar("riot bot")
        assertEquals("r" to "b", c1)
        assertEquals("r" to "i", extractKeyChar("riot"))
    }
}