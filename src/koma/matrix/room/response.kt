package matrix.room

import koma.matrix.UserId
import koma.matrix.epemeral.EphemeralRawEvent
import koma.matrix.sync.Events
import koma.matrix.sync.RawMessage
import koma.matrix.event.GeneralEvent
import matrix.event.room_message.RoomEventType

data class JoinedRoom(
        val unread_notifications: UnreadNotifications?,

        val account_data: Events<GeneralEvent>,
        val ephemeral: Events<EphemeralRawEvent>,
        val state: Events<RawMessage>,
        val timeline: Timeline<RawMessage>
)

data class Timeline<T>(
        val events: List<T>,
        val limited: Boolean?,
        val prev_batch: String?
)

data class InvitedRoom(
        val invited_state: Events<InviteEvent>
)

data class InviteEvent(
        // only these 4 keys are allowed
        // this event doesn't replace previous states
        val sender: UserId,
        val type: String,
        val state_key: String,
        val content: Map<String, Any>
)

data class LeftRoom(
        val state: Events<RawMessage>,
        val timeline: Events<RawMessage>
)

data class UnreadNotifications(
        val highlight_count: Int?,
        val notification_count: Int?
)

/**
 * its usage seems to be decreasing, only needed to get room messages outside of sync api
 */
data class RoomEvent(
        val event_id: String,
        val type: RoomEventType,
        val user_id: UserId,
        val room_id: String,
        val origin_server_ts: Long,
        val content: Map<String, Any>,

        val age: Long?,
        val state_key: String?,
        val prev_content: Map<String, Any>?,
        val unsigned: Map<String, Any>?
) {
    fun toMessage(): RawMessage {
        val msg = RawMessage(
                event_id = event_id,
                type = type,
                sender = user_id,
                origin_server_ts = origin_server_ts,
                content = content,
                age = age,
                state_key = state_key,
                prev_content = prev_content,
                txn_id = null
        )
        return msg
    }
}
