package link.continuum.database.models

import io.requery.*
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.RawJson
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonElementSerializer
import link.continuum.database.jsonMain
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

abstract class DeserializableEventRecord {
    abstract val json: String
    protected var _event: RoomEvent? = null
    fun getEvent(): RoomEvent? {
        val e = _event
        if (e != null) return e
        val decoded = jsonMain.decodeFromString(RoomEvent.serializer(), json)
        _event = decoded
        return decoded
    }
    fun initializeEvent(event: RoomEvent) {
        this._event = event
    }
}

@Entity
@Table(name = "room_chrono_events")
abstract class RoomEventRow: Persistable, DeserializableEventRecord() {
    @get:Key
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    abstract var room_id: String
    @get:Key
    abstract var server_time: Long

    @get:Key
    @get:Index("event_id_index")
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    abstract var event_id: String

    /**
     * encoded event
     */
    @get:Column(nullable = false, length = Int.MAX_VALUE)
    abstract override var json: String

    /**
     * whether the message that comes after this one is in the db
     */
    @get:Column(nullable = false)
    abstract var following_stored: Boolean
    /**
     * pagination token to fetch following events
     */
    @get:Column(length = Int.MAX_VALUE, nullable = true)
    abstract var following_batch: String?
    @get:Column(nullable = false)
    abstract var preceding_stored: Boolean
    @get:Column(length = Int.MAX_VALUE, nullable = true)
    abstract var preceding_batch: String?
}

fun newRoomEventRow(event: RoomEvent, roomId: RoomId, json: String): RoomEventRow {
    val row = RoomEventRowEntity()
    row.room_id = roomId.id
    row.event_id = event.event_id
    row.json = json
    row.initializeEvent(event)
    row.server_time = event.origin_server_ts
    row.following_stored = true
    row.preceding_stored = true
    return row
}

fun List<RawJson<RoomEvent>>.toEventRowList(roomId: RoomId): List<RoomEventRow> {
    val rows = this.map {
        newRoomEventRow(it.value, roomId, it.stringify())
    }
    rows.firstOrNull()?.preceding_stored = false
    rows.lastOrNull()?.following_stored = false
    return rows
}
