package koma.controller.events_processing

import koma.controller.room.applyUpdate
import koma.controller.room.handle_ephemeral
import koma.matrix.epemeral.parse
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.storage.config.profile.Profile
import koma.storage.message.AppendSync
import koma_app.appState.sortMembersInEachRoom
import kotlinx.coroutines.experimental.launch
import matrix.room.InvitedRoom
import matrix.room.JoinedRoom
import matrix.room.LeftRoom
import model.Room


fun process_presence(message: PresenceMessage) {
    message.getUserState()?.let {
        it.present.set(message.content.presence)
        val laa = message.content.last_active_ago
        if (laa is Number)
            it.lastActiveAgo.set(laa.toLong())
    }
}

private fun Profile.handle_joined_room(roomid: RoomId, data: JoinedRoom) {
    val room = this.joinRoom(roomid)

    data.state.events.forEach { room.applyUpdate(it) }
    val timeline = data.timeline
    timeline.events.forEach { room.applyUpdate(it) }
    launch {
        room.messageManager.chan.send(AppendSync(timeline))
    }
    room.handle_ephemeral(data.ephemeral.events.map { it.parse() }.filterNotNull())
    // TODO:  account_data
}

fun Profile.joinRoom(roomid: RoomId): Room {
    val room = this.roomStore.add(roomid)
    return room
}

private fun Profile.leaveLeftRooms(roomid: RoomId, leftRoom: LeftRoom) {
    this.roomStore.remove(roomid)
}

private fun handle_invited_room(roomid: String, data: InvitedRoom) {
    println("TODO: handle room invitation $data")
}

fun Profile.processEventsResult(syncRes: SyncResponse) {
    syncRes.presence.events.forEach { process_presence(it) }
    // TODO: handle account_data
    syncRes.rooms.join.forEach{ rid, data -> this.handle_joined_room(RoomId(rid), data)}
    syncRes.rooms.invite.forEach{ rid, data -> handle_invited_room(rid, data)}
    syncRes.rooms.leave.forEach { id, leftroom -> this.leaveLeftRooms(id, leftroom) }
    // there's also left rooms

    sortMembersInEachRoom()
}
