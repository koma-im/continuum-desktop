package koma.matrix.event.room_message

import koma.matrix.UserId
import koma.matrix.event.EventId
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.chat.MessageUnsigned
import koma.matrix.event.room_message.state.RoomRedactContent
import koma.matrix.json.MoshiInstance
import matrix.event.room_message.RoomEventType

// try to make moshi return different kind of objects depending on a key

abstract class RoomEvent(
        val event_id: EventId,
        val origin_server_ts: Long,
        open val type: RoomEventType?
): Comparable<RoomEvent>{
    override fun compareTo(other: RoomEvent): Int {
        return this.origin_server_ts.compareTo(other.origin_server_ts)
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val om = other as RoomEvent
        return this.event_id == om.event_id
    }

    override fun hashCode() = this.event_id.hashCode()

    companion object {
        private val adapter = MoshiInstance.roomEventAdapter
        private val adapterIndented = adapter.indent("    ")
    }

    fun toJson(indent: Boolean = false): String{
        val adapter = if (indent) adapterIndented else adapter
        val json = adapter.toJson(this)
        return json
    }
}

class MRoomMessage(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val unsigned: MessageUnsigned?,
        /**
         * sometimes content can be as empty as {}
         */
        val content: M_Message?): RoomEvent(event_id, origin_server_ts, type=RoomEventType.Message)

class MRoomRedaction(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val redacts: String,
        val content: RoomRedactContent): RoomEvent(event_id, origin_server_ts, type = RoomEventType.Redaction)

class MRoomUnrecognized(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId?,
        val state_key: String?,
        val unsigned: MessageUnsigned?,
        val content: Map<String, Any>?
): RoomEvent(event_id, origin_server_ts, type = null) {
    override fun toString(): String {
        return "MRoomUnrecognized(prev_content=$prev_content, sender=$sender, state_key=$state_key, unsigned=$unsigned, content=$content)"
    }
}

