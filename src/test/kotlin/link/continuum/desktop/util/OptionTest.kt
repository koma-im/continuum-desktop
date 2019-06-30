package link.continuum.desktop.util

import kotlin.test.*

internal class OptionTest {

    @Test
    fun test1() {
        val a = Some(1)
        val b = Some(1)
        val c = Some(Some(1))
        assertEquals<Option<Int>>(Some(1), Some(1))
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a.isPresent)
        assertFalse(a.isEmpty)
        assertNotEquals(a as Any, c as Any)
        assertEquals(None<Int>(), None())
    }
}
