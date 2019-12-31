package link.continuum.desktop.database.models

import io.requery.kotlin.eq
import link.continuum.database.models.ServerAddress
import link.continuum.database.models.ServerAddressEntity
import link.continuum.database.openStore
import org.junit.jupiter.api.AfterAll
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class ServerKtTest {
    @Test
    fun testAddServer() {
        val server = ServerAddressEntity()
        server.name = "matrix.org"
        server.address = "https://matrix.org/"
        server.lastUse = 7
        assertEquals(0, data.count(ServerAddress::class).get().value())
        data.upsert(server)
        assertEquals(1, data.count(ServerAddress::class).get().value())
        val s1 = ServerAddressEntity()
        s1.name = "matrix.org"
        s1.address = "https://matrix.org/"
        data.update(s1)
        assertEquals(1, data.count(ServerAddress::class).get().value())
        val s = data.select(ServerAddress::class).where(
                ServerAddress::name.eq("matrix.org")
                        .and(ServerAddress::address.eq("https://matrix.org/"))
        ).get().first()
        assertEquals(7, s.lastUse)
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
