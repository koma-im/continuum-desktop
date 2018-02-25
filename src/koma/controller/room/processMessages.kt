package koma.controller.room

import koma.matrix.epemeral.EphemeralEvent
import koma.matrix.epemeral.TypingEvent
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.*
import koma.matrix.room.participation.Membership
import koma.storage.users.UserStore
import koma_app.appState
import koma_app.appState.apiClient
import model.Room

fun Room.handle_ephemeral(events: List<EphemeralEvent>) {
    events.forEach { message ->
        when (message) {
            is TypingEvent -> this.users_typing.setAll(message.user_ids)
        }
    }
}

fun Room.applyUpdate(update: RoomEvent) {
    if (update !is RoomStateEvent) return
    when (update) {
        is MRoomMember -> this.updateMember(update)
        is MRoomAliases -> {
            this.aliases.setAll(update.content.aliases)
        }
        is MRoomAvatar -> this.iconURL=update.content.url
        is MRoomCanonAlias -> this.setCanonicalAlias(update.content.alias)
        is MRoomJoinRule -> this.joinRule = update.content.join_rule
        is MRoomHistoryVisibility -> this.histVisibility = update.content.history_visibility
        is MRoomPowerLevels -> this.updatePowerLevels(update.content)
        is MRoomCreate -> { }
        is MRoomPinnedEvents -> {}
        is MRoomTopic -> {}
        is MRoomName -> {}
        is MRoomGuestAccess -> {}
    }
}


fun Room.updateMember(update: MRoomMember) {
    when(update.content.membership)  {
        Membership.join -> {
            val senderid = update.sender
            val user = UserStore.getOrCreateUserId(senderid)
            update.content.avatar_url?.let { user.avatar = it }
            update.content.displayname?.let { user.name=it }
            this.makeUserJoined(user)
        }
        Membership.leave -> {
            this.removeMember(update.sender)
            if (apiClient?.userId == update.sender) {
                val roomStore = appState.apiClient?.profile?.roomStore
                roomStore?.remove(this.id)
            }
        }
        Membership.ban -> {
            this.removeMember(update.sender)
        }
        else -> {
            println("todo: handle membership ${update.content}")
        }
    }
}
