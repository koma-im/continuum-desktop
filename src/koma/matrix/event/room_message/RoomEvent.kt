package koma.matrix.event.room_message

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.UserIdAdapter
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.chat.MessageUnsigned
import koma.matrix.event.room_message.state.RoomRedactContent
import koma.matrix.room.naming.RoomAliasAdapter

// try to make moshi return different kind of objects depending on a key

abstract class RoomEvent(
        val event_id: String,
        val origin_server_ts: Long
): Comparable<RoomEvent>{
    override fun compareTo(other: RoomEvent): Int {
        return this.origin_server_ts.compareTo(other.origin_server_ts)
    }

    override fun equals(other: Any?): Boolean {
        val om = other as RoomEvent
        return this.event_id == om.event_id
    }

    override fun hashCode() = this.event_id.hashCode()

    companion object {
        private val adapter = Moshi.Builder()
                .add(UserIdAdapter())
                .add(RoomAliasAdapter())
                .add(getPolyRoomEventAdapter())
                .build().adapter(RoomEvent::class.java)
    }

    /**
     * convert to string to storage
     * omit age, which is temporary
     */
    fun toJson(): String{
        return adapter.toJson(this)
    }
}

class MRoomMessage(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val unsigned: MessageUnsigned?,
        /**
         * sometimes content can be as empty as {}
         */
        val content: M_Message?): RoomEvent(event_id, origin_server_ts)

class MRoomRedaction(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val redacts: String,
        val content: RoomRedactContent): RoomEvent(event_id, origin_server_ts)

class MRoomUnrecognized(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId?,
        val state_key: String?,
        val unsigned: MessageUnsigned?,
        val content: Map<String, Any>?): RoomEvent(event_id, origin_server_ts)

