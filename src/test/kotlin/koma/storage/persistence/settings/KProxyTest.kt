package koma.storage.persistence.settings

import koma.storage.persistence.settings.encoding.KProxy
import link.continuum.database.models.deserialize
import link.continuum.database.models.serialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class KProxyTest {
    @Test
    fun testKProxy() {
        assertEquals(KProxy.Direct, KProxy.Direct)
        assertTrue { listOf(KProxy.Direct).contains(KProxy.Direct) }
    }

    @Test
    fun testKProxySerial() {
        val x = KProxy.parse("http 127.0.0.1 8080").get()
        val b = serialize(x)!!
        val y = deserialize<KProxy>(b)
        assertEquals(x, y)
    }
}
