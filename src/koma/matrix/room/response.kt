package matrix.room

import koma.matrix.UserId
import koma.matrix.epemeral.GeneralEvent
import koma.matrix.sync.Events
import model.Message

data class JoinedRoom(
        val unread_notifications: UnreadNotifications?,

        val account_data: Events<GeneralEvent>,
        val ephemeral: Events<GeneralEvent>,
        val state: Events<Message>,
        val timeline: Timeline<Message>
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
        val state: Events<Message>,
        val timeline: Events<Message>
)

data class UnreadNotifications(
        val highlight_count: Int?,
        val notification_count: Int?
)
