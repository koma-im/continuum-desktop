package link.continuum.desktop.database.models

import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import link.continuum.database.models.*
import link.continuum.database.openStore
import org.junit.jupiter.api.AfterAll
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class RoomKtTest {
    @Test
    fun testDefaultRoomPowerSettings() {
        val room0 = RoomId("matrix.org", "room0")
        assertEquals(0f, defaultRoomPowerSettings(room0).stateDefault)
    }

    @Test
    fun testSaveRoomSettings() {
        val dir = Files.createTempDirectory("continuum-test").resolve("testRoomSettings")
        val data = openStore(dir.toString())
        val room0 = RoomId("matrix.org", "room0")
        val settings: RoomSettings = RoomSettingsEntity()
        settings.roomId = room0.id
        settings.historyVisibility = HistoryVisibility.Invited
        settings.joinRule = RoomJoinRules.Invite
        settings.visibility = RoomVisibility.Private
        data.insert(settings)
        data.close()

        val d1 = openStore(dir.toString())
        val c1 = RoomSettings::roomId.eq(room0.id)
        val s1 = d1.select(RoomSettings::class).where(c1).get().first()
        assertEquals(HistoryVisibility.Invited, s1.historyVisibility)
        assertEquals(RoomJoinRules.Invite, s1.joinRule)
        assertEquals(RoomVisibility.Private, s1.visibility)
    }

    @Test
    fun testSaveUserPowerLevels() {
        val room0 = RoomId("matrix.org", "room0")
        val u0 = UserId("@user0:matrix.org")
        saveUserPowerLevels(data, room0, mapOf(u0 to 6f))
        val c1 = UserPower::room.eq(room0.id)
        val p = data.select(UserPower::class).where(UserPower::person.eq(u0.str).and(c1)).get().firstOrNull()?.power
        assertEquals(6, p)
    }

    @Test
    fun testGetRoomMemberPower() {
        val room0 = RoomId("matrix.org", "room0")
        val u0 = UserId("@user0:matrix.org")
        assertEquals(0f, getRoomMemberPower(data, roomId = room0, userId = u0))
        val default: RoomPowerSettings = RoomPowerSettingsEntity()
        default.roomId = room0.id
        default.usersDefault = 3.1f
        data.upsert(default)
        assertEquals(3.1f, getRoomMemberPower(data, roomId = room0, userId = u0))
        data.upsert(default)
        assertEquals(3.1f, getRoomMemberPower(data, roomId = room0, userId = u0))
        saveUserPowerLevels(data, room0, mapOf(u0 to 7f))
        assertEquals(7f, getRoomMemberPower(data, roomId = room0, userId = u0))
    }

    @Test
    fun testGetChangeStateAllowed() {
    }

    @Test
    fun testSavePowerSettings() {
    }

    @Test
    fun testSaveEventPowerLevels() {
    }


    @Test
    fun testLoadRoom() {
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
