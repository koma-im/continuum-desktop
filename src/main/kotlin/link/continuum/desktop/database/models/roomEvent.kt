package link.continuum.desktop.database.models

import io.requery.*
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomId
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
interface RoomEventRow: Persistable {
    @get:Key
    var event_id: String

    @get:Key
    var room_id: String

    @get:Index("time_index")
    @get:Column(nullable = false)
    var server_time: Long

    /**
     * encoded event
     */
    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var json: String

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
fun RoomEventRow.getEvent(): RoomEvent? {
    this._event = this._event ?: MoshiInstance.roomEventAdapter.fromJson(this.json)
    if (this._event == null) {
        logger.warn { "event ${this.event_id} decoding failure"}
    }
    return this._event
}

private fun newRoomEventRow(event: RoomEvent, roomId: RoomId, json: String): RoomEventRow {
    val row = RoomEventRowEntity()
    row.room_id = roomId.id
    row.event_id = event.event_id.str
    row._event = event
    row.server_time = event.origin_server_ts
    row.json = json
    row.following_stored = true
    row.preceding_stored = true
    return row
}

fun List<RoomEvent>.toEventRowList(roomId: RoomId): List<RoomEventRow> {
    val rows = this.map { newRoomEventRow(it, roomId, MoshiInstance.roomEventAdapter.toJson(it)) }
    rows.firstOrNull()?.preceding_stored = false
    rows.lastOrNull()?.following_stored = false
    return rows
}
