package link.continuum.desktop.gui.list

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DedupListTest {
    @Test
    fun test() {
        val d = DedupList<String, String>({it})
        assertEquals(0, d.list.size)
        assertThrows<UnsupportedOperationException> { d.list.add("") }
        d.add("a1")
        assertEquals(1, d.list.size)
        d.add("a1")
        assertEquals(1, d.list.size)
        d.addAll(listOf("a2", "a3", "a4", "a5"))
        assertEquals(5, d.list.size)
        d.addAll(listOf("a4", "a5", "a6", "a7"))
        assertEquals(7, d.list.size)
    }
}
