package koma.controller.events

import koma.controller.room.applyUpdate
import koma.controller.room.handle_ephemeral
import koma.gui.view.ChatView
import koma.gui.view.window.auth.uilaunch
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.ephemeral.parse
import koma.matrix.room.InvitedRoom
import koma.matrix.room.JoinedRoom
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.util.matrix.getUserState
import kotlinx.coroutines.*
import link.continuum.database.models._logger
import link.continuum.database.models.removeMembership
import link.continuum.database.models.saveUserInRoom
import link.continuum.desktop.events.handleInvitedRoom
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.Account
import link.continuum.libutil.`?or`
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

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
private suspend fun handle_joined_room(
        roomid: RoomId,
        data: JoinedRoom,
        account: Account,
        appData: AppStore
) {
    val room = appData.roomStore.getOrCreate(roomid, account)
    withContext(UiDispatcher) {
        appData.joinRoom(roomid, account)
        data.state.events.forEach { room.applyUpdate(it, appStore = appData) }
        val timeline = data.timeline
        timeline.events.forEach { room.applyUpdate(it.value, appStore = appData) }
        room.messageManager.appendTimeline(timeline)
    }
    room.handle_ephemeral(data.ephemeral.events.map { it.parse() }.filterNotNull())
    // TODO:  account_data
}


@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
fun processEventsResult(syncRes: SyncResponse,
                        account: Account,
                        appData: AppStore,
                        view: ChatView
                        ) {
    val self = account.userId
    syncRes.presence.events.forEach { process_presence(it) }
    // TODO: handle account_data
    syncRes.rooms.join.forEach{ rid, data ->
        val roomId = RoomId(rid)
        GlobalScope.launch {
            saveUserInRoom(data = appData.database, userId = self, roomId = roomId)
            handle_joined_room(roomId, data, account, appData = appData)
        }
    }
    syncRes.rooms.invite.forEach{ rid, data -> handleInvitedRoom(rid, data, view.invitationsView, account.server) }

    syncRes.rooms.leave.forEach { roomId, leftRoom ->
        removeMembership(data = appData.database, userId = self, roomId = roomId)
    }
    uilaunch {
        syncRes.rooms.leave.forEach { id, leftroom ->
            appData.joinedRoom.removeById(id)
        }
    }
    // there's also left rooms
}
