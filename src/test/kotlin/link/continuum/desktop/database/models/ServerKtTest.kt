package link.continuum.desktop.database.models

import io.requery.kotlin.eq
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.openStore
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ServerKtTest {

    val dir = Files.createTempDirectory("continuum-test")
    val data: KDataStore

    init {
        val path = dir.resolve("test").toString()
        data = openStore(path)
    }
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
}
