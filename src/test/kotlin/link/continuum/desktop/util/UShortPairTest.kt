package link.continuum.desktop.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UShortPairTest {
    @Test
    fun basic() {
        val u0 = UShortPair(UShort.MAX_VALUE, UShort.MAX_VALUE)
        assertEquals(0, UShort.MAX_VALUE.compareTo(u0.first))
        assertEquals(0, UShort.MAX_VALUE.compareTo(u0.second))
        val u1 = UShortPair(0u, 1u)
        assertEquals(0, 0u.compareTo(u1.first))
        assertEquals(0, 1u.compareTo(u1.second))
    }
}
