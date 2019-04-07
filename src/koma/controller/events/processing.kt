package koma.controller.events

import koma.controller.room.applyUpdate
import koma.controller.room.handle_ephemeral
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.ephemeral.parse
import koma.matrix.room.InvitedRoom
import koma.matrix.room.JoinedRoom
import koma.matrix.room.LeftRoom
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.util.matrix.getUserState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

fun process_presence(message: PresenceMessage) {
    message.getUserState()?.let {
        it.present.set(message.content.presence)
        val laa = message.content.last_active_ago
        if (laa is Number)
            it.lastActiveAgo.set(laa.toLong())
    }
}

@ObsoleteCoroutinesApi
private suspend fun handle_joined_room(
        owner: UserId, roomid: RoomId, data: JoinedRoom,
        userData: UserDataStore,
        server: HttpUrl
) {
    val room = addJoinedRoom(owner, roomid)

    data.state.events.forEach { room.applyUpdate(it, userData, server) }
    val timeline = data.timeline
    timeline.events.forEach { room.applyUpdate(it, userData, server) }

    GlobalScope.launch {
        room.messageManager.appendTimeline(timeline)
    }
    room.handle_ephemeral(data.ephemeral.events.map { it.parse() }.filterNotNull())
    // TODO:  account_data
}

/**
 * add a room to the list of joined rooms locally
 * usually used after getting information from the server
 */
fun addJoinedRoom(userId: UserId, roomid: RoomId): Room {
    return appState.store.getAccountRoomStore(userId).add(roomid)
}

private fun leaveLeftRooms(owner: UserId, roomid: RoomId, @Suppress("UNUSED_PARAMETER") _leftRoom: LeftRoom) {
    appState.store.getAccountRoomStore(owner).remove(roomid)
}

private fun handle_invited_room(@Suppress("UNUSED_PARAMETER") _roomid: String, data: InvitedRoom) {
    println("TODO: handle room invitation $data")
}

@ObsoleteCoroutinesApi
fun processEventsResult(owner: UserId, syncRes: SyncResponse, userData: UserDataStore, server: HttpUrl) {
    syncRes.presence.events.forEach { process_presence(it) }
    // TODO: handle account_data
    syncRes.rooms.join.forEach{ rid, data ->
        GlobalScope.launch {
            handle_joined_room(owner, RoomId(rid), data, userData, server)
        }
    }
    syncRes.rooms.invite.forEach{ rid, data -> handle_invited_room(rid, data) }
    syncRes.rooms.leave.forEach { id, leftroom -> leaveLeftRooms(owner, id, leftroom) }
    // there's also left rooms
}
