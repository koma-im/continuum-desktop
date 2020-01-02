package link.continuum.database.models

import io.requery.*
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.RawJson
import koma.matrix.room.naming.RoomId
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
@Table(name = "room_chrono_events")
interface RoomEventRow: Persistable {
    @get:Key
    var room_id: String
    @get:Key
    var server_time: Long

    @get:Key
    @get:Index("event_id_index")
    var event_id: String

    /**
     * encoded event
     */
    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var json: String

    @Deprecated("internal field")
    @get:Transient
    var _event: RoomEvent?

    /**
     * whether the message that comes after this one is in the db
     */
    @get:Column(nullable = false)
    var following_stored: Boolean
    /**
     * pagination token to fetch following events
     */
    var following_batch: String?
    @get:Column(nullable = false)
    var preceding_stored: Boolean
    var preceding_batch: String?
}

/**
 * get decoded event
 */
@Suppress("DEPRECATION")
fun RoomEventRow.getEvent(): RoomEvent? {
    this._event = this._event ?: RoomEvent.parseOrNull(this.json)
    if (this._event == null) {
        logger.warn { "event ${this.event_id} decoding failure"}
    }
    return this._event
}

@Suppress("DEPRECATION")
fun newRoomEventRow(event: RoomEvent, roomId: RoomId, json: String): RoomEventRow {
    val row = RoomEventRowEntity()
    row.room_id = roomId.id
    row.event_id = event.event_id
    row._event = event
    row.server_time = event.origin_server_ts
    row.json = json
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
