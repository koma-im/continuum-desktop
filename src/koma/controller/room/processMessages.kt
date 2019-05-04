package koma.controller.room

import koma.koma_app.AppStore
import koma.koma_app.appState.apiClient
import koma.matrix.event.ephemeral.EphemeralEvent
import koma.matrix.event.ephemeral.TypingEvent
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.*
import koma.matrix.room.participation.Membership
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.http.mapMxc
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

fun Room.handle_ephemeral(events: List<EphemeralEvent>) {
    events.forEach { message ->
        when (message) {
            is TypingEvent -> {}
        }
    }
}

suspend fun Room.applyUpdate(
        update: RoomEvent,
        server: HttpUrl,
        appStore: AppStore
) {
    val room = this
    if (update !is RoomStateEvent) return
    when (update) {
        is MRoomMember -> this.updateMember(update, server, appStore)
        is MRoomAliases -> {
            withContext(UiDispatcher) {
                room.aliases.addAll(0, update.content.aliases)
            }
        }
        is MRoomAvatar -> HttpUrl.parse(update.content.url)?.let { this.avatar.set(it) }
        is MRoomCanonAlias -> {
            withContext(UiDispatcher) {
                room.canonicalAlias.set(update.content.alias)
            }
        }
        is MRoomJoinRule -> this.joinRule = update.content.join_rule
        is MRoomHistoryVisibility -> this.histVisibility = update.content.history_visibility
        is MRoomPowerLevels -> this.updatePowerLevels(update.content)
        is MRoomCreate -> { }
        is MRoomPinnedEvents -> {}
        is MRoomTopic -> {}
        is MRoomName -> {
            withContext(UiDispatcher) {
                room.name.set(update.content.name)
            }
        }
        is MRoomGuestAccess -> {}
    }
}


@ExperimentalCoroutinesApi
suspend fun Room.updateMember(
        update: MRoomMember,
        server: HttpUrl,
        appStore: AppStore
) {
    val room = this
    val userData = appStore.userData
    when(update.content.membership)  {
        Membership.join -> {
            val senderid = update.sender
            update.content.avatar_url?.let {
                val u = mapMxc(it, server)
                if (u == null) logger.error { "invalid avatar url in ${update.content.avatar_url}"}
                u
            }?.let {
                userData.updateAvatarUrl(senderid, it, update.origin_server_ts)
            }
            update.content.displayname?.let {
                userData.updateName(update.sender, it, update.origin_server_ts)
            }
            withContext(UiDispatcher) {
                room.makeUserJoined(senderid)
            }
        }
        Membership.leave -> {
            withContext(UiDispatcher) {
                room.removeMember(update.sender)
                if (apiClient?.userId == update.sender) {
                    appStore.joinedRoom.removeById(room.id)
                }
            }
        }
        Membership.ban -> {
            withContext(UiDispatcher) {
                room.removeMember(update.sender)
            }
        }
        else -> {
            println("todo: handle membership ${update.content}")
        }
    }
}

