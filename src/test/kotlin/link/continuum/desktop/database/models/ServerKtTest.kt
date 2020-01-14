package link.continuum.desktop.database.models

import io.requery.kotlin.eq
import link.continuum.database.openStore
import link.continuum.desktop.database.KeyValueStore
import org.h2.mvstore.MVStore
import org.junit.jupiter.api.AfterAll
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class ServerKtTest {
    @Test
    fun testAddServer() {
        val mvStore = MVStore.open(dir.resolve("mv").toString())
        val keyValueStore = KeyValueStore(mvStore)
        val addressMap = keyValueStore.serverToAddress
        assertEquals(0, addressMap.size)
        addressMap.put("matrix.org", "https://matrix.org/")
        assertEquals(1, addressMap.size)
        addressMap["matrix.org"] = "https://matrix.org"
        assertEquals(1, addressMap.size)
        assertEquals("https://matrix.org", addressMap["matrix.org"])
    }

    companion object {
        val tmp = Files.createTempDirectory("continuum-test")
        val dir = tmp.resolve("test")
        val data = openStore(dir.toString())
        @AfterAll
        @JvmStatic
        fun teardown() {
            tmp.toFile().deleteRecursively()
        }
    }
}
