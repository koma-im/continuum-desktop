package koma.matrix.sync

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.UserIdAdapter
import matrix.event.room_message.RoomEventType

/**
 * message received as a dict from the server
 */
data class RawMessage(
        val age: Long?,
        val event_id: String,
        val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val type: RoomEventType,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Map<String, Any>) {
    companion object {
        private val adapter = Moshi.Builder().add(UserIdAdapter()).build().adapter(RawMessage::class.java)
    }

    /**
     * convert to string to storage
     * omit age, which is temporary
     */
    fun toJson(): String{
        return adapter.toJson(this.copy(age=null))
    }
}

