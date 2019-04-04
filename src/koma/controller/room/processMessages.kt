package koma.controller.room

import koma.koma_app.appState
import koma.koma_app.appState.apiClient
import koma.matrix.event.ephemeral.EphemeralEvent
import koma.matrix.event.ephemeral.TypingEvent
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.*
import koma.matrix.room.participation.Membership
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

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
        is MRoomAvatar -> HttpUrl.parse(update.content.url)?.let { this.iconURL = it }
        is MRoomCanonAlias -> this.setCanonicalAlias(update.content.alias)
        is MRoomJoinRule -> this.joinRule = update.content.join_rule
        is MRoomHistoryVisibility -> this.histVisibility = update.content.history_visibility
        is MRoomPowerLevels -> this.updatePowerLevels(update.content)
        is MRoomCreate -> { }
        is MRoomPinnedEvents -> {}
        is MRoomTopic -> {}
        is MRoomName -> { this.name.set(update.content.name) }
        is MRoomGuestAccess -> {}
    }
}


fun Room.updateMember(update: MRoomMember) {
    when(update.content.membership)  {
        Membership.join -> {
            val senderid = update.sender
            val user = appState.store.userStore.getOrCreateUserId(senderid)
            update.content.avatar_url?.let {
                val u = HttpUrl.parse(it)
                if (u == null) logger.error { "invalid avatar url in ${update.content}"}
                u
            }?.let {
                user.setAvatar(it, update.origin_server_ts)
            }
            update.content.displayname?.let {
                user.setName(it, update.origin_server_ts)
            }
            this.makeUserJoined(user)
        }
        Membership.leave -> {
            this.removeMember(update.sender)
            if (apiClient?.userId == update.sender) {
                appState.accountRoomStore()?.remove(this.id)
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
