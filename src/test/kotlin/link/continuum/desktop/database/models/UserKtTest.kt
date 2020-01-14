package link.continuum.desktop.database.models

import koma.matrix.UserId
import link.continuum.database.models.*
import link.continuum.database.openStore
import link.continuum.desktop.database.KeyValueStore
import org.h2.mvstore.MVStore
import org.junit.jupiter.api.AfterAll
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class UserKtTest {
    @Test
    fun testInsertUser() {
        val mvStore = MVStore.open(dir.resolve("mv").toString())
        val keyValueStore = KeyValueStore(mvStore)
        val m = keyValueStore.userToToken
        m["@user:matrix.org"] = "SECRET_TOKEN"
        assertEquals("SECRET_TOKEN", m["@user:matrix.org"])
    }

    @Test
    fun saveToken() {
    }

    @Test
    fun getToken() {
    }

    @Test
    fun testUserNick() {
        assertEquals(data.count(UserNickname::class).get().value(), 0)

        val u0 = UserId("@user:matrix.org")
        saveUserNick(data, u0, "nick1", 99)
        assertEquals(1, data.count(UserNickname::class).get().value())
        assertEquals("nick1", getLatestNick(data, u0)!!.nickname)

        saveUserNick(data, u0, "nick0", 90)
        assertEquals(2, data.count(UserNickname::class).get().value())
        assertEquals("nick1", getLatestNick(data, u0)!!.nickname)

        saveUserNick(data, u0, "nick1", 99)
        assertEquals(2, data.count(UserNickname::class).get().value())

        val t: UserNickname = UserNicknameEntity()
        t.owner = u0.str
        t.nickname = "nick1"
        t.since = 99
        data.insert(t)
        assertEquals(3, data.count(UserNickname::class).get().value())
    }

    @Test
    fun testUserAvatar() {
        assertEquals(data.count(UserAvatar::class).get().value(), 0)

        val u0 = UserId("@user:matrix.org")
        saveUserAvatar(data, u0, "avatar1", 99)
        assertEquals(1, data.count(UserAvatar::class).get().value())
        assertEquals("avatar1", getLatestAvatar(data, u0)!!.avatar)

        saveUserAvatar(data, u0, "avatar0", 90)
        assertEquals(2, data.count(UserAvatar::class).get().value())
        assertEquals("avatar1", getLatestAvatar(data, u0)!!.avatar)

        saveUserAvatar(data, u0, "avatar1", 99)
        assertEquals(2, data.count(UserAvatar::class).get().value())

        val t: UserAvatar = UserAvatarEntity()
        t.key = u0.str
        t.avatar = "avatar1"
        t.since = 99
        data.insert(t)
        assertEquals(3, data.count(UserAvatar::class).get().value())
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
