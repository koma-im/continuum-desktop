package link.continuum.desktop.database.models

import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.room.naming.RoomId
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