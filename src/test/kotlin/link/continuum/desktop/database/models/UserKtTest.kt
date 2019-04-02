package link.continuum.desktop.database.models

import koma.matrix.UserId
import link.continuum.desktop.database.openStore
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UserKtTest {
    val dir = Files.createTempDirectory("continuum-test").resolve("test")
    val data = openStore(dir.toString())

    @Test
    fun testInsertUser() {
        val userId = UserId("@user:matrix.org")
        saveToken(data, userId, "SECRET_TOKEN")
        assertEquals(getToken(data, userId), "SECRET_TOKEN")

    }

    @Test
    fun saveToken() {
    }

    @Test
    fun getToken() {
    }
}
