package koma.gui.element.emoji.icon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CacheKtTest {
    @Test
    fun testEmojiCode() {
        assertEquals("1f383", getEmojiCode("🎃"))
        assertEquals("1f3ae", getEmojiCode("🎮"))
        assertEquals("1f3f3-1f308", getEmojiCode("🏳️‍🌈"))
        assertEquals("1f1e6-1f1e8", getEmojiCode("🇦🇨"))
    }

}
