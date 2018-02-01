package koma.matrix.event.room_message.state.member

import koma.matrix.UserId
import koma.matrix.room.participation.Membership

class RoomMemberContent(
        val membership: Membership,
        val avatar_url: String?,
        val displayname: String?,
        val is_direct: Boolean?,
        val third_party_invite: Invite?
)

class PrevContent(
        val avatar_url: String?,
        val membership: Membership?,
        val displayname: String?
)

class RoomMemberUnsigned(
        val age: Int,
        val prev_content: PrevContent?,
        val prev_sender: UserId?,
        val replaces_state: String?
)

class Invite(
        val displayname: String
        /**
         * signed should be ignored by clients
         */
        // val signed
)

/**
 * m.room.member may also include an invite_room_state key outside
 * the content key. If present, this contains an array of StrippedState Events
 */
class StrippedState(
        val state_key: String,
        val type: String,
        val content: Map<String, Any>
)

