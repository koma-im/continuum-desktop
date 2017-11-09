package koma.controller.events_processing

import koma.controller.room.applyUpdate
import koma.controller.room.handle_ephemeral
import koma.matrix.epemeral.parse
import koma.matrix.event.parse
import koma.matrix.event.room_message.timeline.parse
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.storage.message.appendTimeline
import koma.storage.rooms.UserRoomStore
import koma_app.appState.sortMembersInEachRoom
import matrix.room.InvitedRoom
import matrix.room.JoinedRoom


fun process_presence(message: PresenceMessage) {
    message.getUserState()?.let {
        it.present.set(message.content.presence)
        val laa = message.content.last_active_ago
        if (laa is Number)
            it.lastActiveAgo.set(laa.toLong())
    }
}

private fun handle_joined_room(roomid: String, data: JoinedRoom) {
    val room = UserRoomStore.add(roomid)

    data.state.events.map { it.parse() }.forEach { room.applyUpdate(it) }
    val timeline = data.timeline.parse()
    timeline.events.forEach { room.applyUpdate(it) }
    room.messageManager.appendTimeline(timeline)

    room.handle_ephemeral(data.ephemeral.events.map { it.parse() }.filterNotNull())
    // TODO:  account_data
}

private fun handle_invited_room(roomid: String, data: InvitedRoom) {
    println("TODO: handle room invitation $data")
}

fun processEventsResult(syncRes: SyncResponse) {
    syncRes.presence.events.forEach { process_presence(it) }
    // TODO: handle account_data
    syncRes.rooms.join.forEach{ rid, data -> handle_joined_room(rid, data)}
    syncRes.rooms.invite.forEach{ rid, data -> handle_invited_room(rid, data)}
    // there's also left rooms

    sortMembersInEachRoom()
}
