package link.continuum.desktop.database.models

import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomCreate
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.state.RoomCreateContent
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.newRoomEventRow
import link.continuum.database.openStore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files

internal class RoomEventKtTest {

    @Test
    fun saveEvent() {
        val event = MRoomMessage("eid", 127L, sender = UserId("uid"))
        val row = newRoomEventRow(event, RoomId("rid1"), "{}")
        data.upsert(row)
        val r = data.select(RoomEventRow::class).get().first()
        assertEquals("{}", r.json)
        assertSame(event, r.getEvent())
        assertEquals("rid1", r.room_id)
        val event1 = MRoomCreate("eid", 127L, sender = UserId("uv1"), content = RoomCreateContent(UserId("x")))
        val row1 = newRoomEventRow(event1, RoomId("rid1"), "{\"k\": 12}")
        data.upsert(row1)
        val rs = data.select(RoomEventRow::class).get().toList()
        assertEquals(1, rs.size)
        val r1 = rs[0]
        assertSame(event1, r1.getEvent())
    }

    @Test
    fun event1() {
        val event = MRoomMessage("eid", 127L, sender = UserId("uid"))
        val row = newRoomEventRow(event, RoomId("rid1"), "{}")
        data.upsert(row)
        val r = data.select(RoomEventRow::class).get().first()
        assertEquals("{}", r.json)
        assertSame(event, r.getEvent())
        assertEquals("rid1", r.room_id)
        val event1 = MRoomCreate("eid", 127L, sender = UserId("uv1"), content = RoomCreateContent(UserId("x")))
        val originalJson = JsonObject(mapOf(
                "META-INF" to JsonPrimitive(3446502),
                "room_id" to JsonPrimitive("CacheKtTest.kt")
        ))
        val row1 = newRoomEventRow(event1, RoomId("rid1"), "{\"k\": 12}")
        data.upsert(row1)
        val rs = data.select(RoomEventRow::class).get().toList()
        assertEquals(1, rs.size)
        val r1 = rs[0]
        assertSame(event1, r1.getEvent())
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