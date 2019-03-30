package link.continuum.desktop.database.models

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConfigurationKVKtTest

class Test {
    @Test
    fun testSerialization() {
        val x = "hello"
        val b = serialize(x)!!
        val y = deserialize<String>(b)
        assertEquals(x, y)
        val x1 = listOf("hello")
        val b1 = serialize(x1)!!
        val y1 = deserialize<List<String>>(b1)
        assertEquals(x1, y1)
    }
}
