package link.continuum.desktop.util

import kotlin.test.*

internal class OptionTest {

    @Test
    fun test1() {
        val a = Some(1)
        val b = Some(1)
        val c = Some(Some(1))
        assertEquals(Some(1), Some(1))
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a.isSome)
        assertFalse(a.isNone)
        assertNotEquals(a as Any, c as Any)
        assertEquals(None<Int>(), None())
        assertEquals(None<String>() as Option<Any>, None<Int>())
        assertEquals<Option<Any>>(None<String>(), None<Int>())
        assertNotSame(None<Int>(), None<Int>()) // turn out to be different
    }
}
