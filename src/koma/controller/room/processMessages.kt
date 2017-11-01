package koma.controller.room

import koma.matrix.epemeral.EphemeralEvent
import koma.matrix.epemeral.TypingEvent
import koma.matrix.event.room_message.*
import koma.storage.rooms.UserRoomStore
import koma_app.appState.apiClient
import model.Room

fun Room.handle_ephemeral(events: List<EphemeralEvent>) {
    events.forEach { message ->
        when (message) {
            is TypingEvent -> this.users_typing.setAll(message.user_ids)
        }
    }
}

fun Room.applyUpdate(update: RoomMessage) {
    when (update) {
        is MemberJoinMsg -> this.makeUserJoined(update.sender)
        is MemberLeave -> {
            this.removeMember(update.sender.id)
            if (apiClient?.userId == update.sender.id) {
                UserRoomStore.remove(this.id)
            }
        }
        is MemberBan -> this.removeMember(update.sender.id)
        is RoomAliasUpdate -> this.aliases.addAll(update.aliases)
        is RoomIconUpdate -> this.iconURL.set(update.url)
        is RoomCanonicalAlias -> this.setCanonicalAlias(update.canonicalAlias)
        is RoomJoinRuleUpdate -> this.joinRule = update.rule
        is RoomHistoryVisibilityUpdate -> this.histVisibility = update.visibility
        is RoomPowerLevel -> this.updatePowerLevels(update)
        // only appears in the timeline, not part of the state of a room
        is MemberUpdateMsg, // actually, this only seem to be related to users, but
        is RoomCreationMsg,
        is ChatMessage
        -> {}
    }
}

