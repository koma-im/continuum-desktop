package matrix.room

import koma.matrix.UserId
import koma.matrix.epemeral.EphemeralRawEvent
import koma.matrix.event.GeneralEvent
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.sync.Events
import koma.matrix.sync.RawMessage

data class JoinedRoom(
        val unread_notifications: UnreadNotifications?,

        val account_data: Events<GeneralEvent>,
        val ephemeral: Events<EphemeralRawEvent>,
        val state: Events<RoomEvent>,
        val timeline: Timeline<RoomEvent>
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
