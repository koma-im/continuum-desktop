package koma.storage.persistence.settings

import koma.storage.persistence.settings.encoding.toCSV
import koma.storage.persistence.settings.encoding.toProxyResult
import koma.util.getOrThrow
import kotlinx.serialization.UnstableDefault
import java.net.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableDefault
internal class KProxyTest {

    @Test
    fun test() {
        val p = "http,127.0.0.1,8080".toProxyResult().getOrThrow()
        assertEquals(Proxy.Type.HTTP, p.type())
        assertEquals("127.0.0.1:8080", p.address().toString())
        assertEquals("HTTP,127.0.0.1,8080",    p.toCSV())
    }
}
