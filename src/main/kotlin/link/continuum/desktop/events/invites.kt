package link.continuum.desktop.events

import koma.matrix.UserId
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.event.room_message.state.RoomJoinRulesContent
import koma.matrix.event.room_message.state.RoomNameContent
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.room.InvitedRoom
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.Membership
import link.continuum.desktop.gui.list.InvitationsView
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

internal fun handleInvitedRoom(
        roomId: RoomId, data: InvitedRoom,
        view: InvitationsView,
        server: HttpUrl
) {
    logger.debug { "Invited to $roomId, data $data" }
    view.add(InviteData(data, roomId), server)
}

/**
 * for list view
 */
class InviteData(src: InvitedRoom, val id: RoomId) {
    var roomAvatar: String? = null
    val roomDisplayName: String

    var inviterAvatar: String? = null
    var inviterName: String? = null

    var inviterId: UserId? = null

    private var roomCanonAlias: RoomAlias? = null
    private var roomName: String? = null

    init {
        val userNames = mutableMapOf<UserId, String>()
        val userAvatars = mutableMapOf<UserId, String>()
        for (e in src.invite_state.events) {
            val c = e.content
            when (c) {
                is RoomMemberContent -> {
                    when (c.membership) {
                        Membership.join -> {
                            c.avatar_url?.let { userAvatars.put(e.sender, it) }
                            c.displayname?.let { userNames.put(e.sender, it) }
                        }
                        Membership.invite -> {
                            inviterId = e.sender
                        }
                        else -> {}
                    }
                }
                is RoomCanonAliasContent -> roomCanonAlias = c.alias
                is RoomNameContent -> roomName = c.name
                is RoomAvatarContent -> roomAvatar = c.url
                is RoomJoinRulesContent -> {}
            }
        }
        roomDisplayName = roomName ?: roomCanonAlias.toString() ?: id.id
        inviterId?.let {
            inviterAvatar = userAvatars.get(it)
            inviterName = userNames.get(it)
        }
    }

    override fun toString(): String {
        return "InviteData(roomAvatar=$roomAvatar, roomDisplayName='$roomDisplayName', inviterAvatar=$inviterAvatar, inviterName=$inviterName, inviterId=$inviterId, roomCanonAlias=$roomCanonAlias, roomName=$roomName)"
    }
}