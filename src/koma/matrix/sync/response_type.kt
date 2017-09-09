package koma.matrix.sync

import koma.matrix.user.presence.PresenceMessage
import matrix.room.InvitedRoom
import matrix.room.JoinedRoom
import matrix.room.LeftRoom

data class Events<T>(
        val events: List<T>
)

data class SyncResponse(
        val next_batch: String,
        val presence: Events<PresenceMessage>,
        val account_data: Events<Map<String, Any>>,
        val rooms: RoomsResponse
)

data class RoomsResponse(
        val join: Map<String, JoinedRoom>,
        val invite: Map<String, InvitedRoom>,
        val leave: Map<String, LeftRoom>
)
