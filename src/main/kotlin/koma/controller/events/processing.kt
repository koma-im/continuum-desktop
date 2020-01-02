package koma.controller.events

import koma.controller.room.applyUpdate
import koma.controller.room.handle_ephemeral
import koma.gui.view.ChatView
import koma.koma_app.AppStore
import koma.matrix.event.ephemeral.parse
import koma.matrix.room.JoinedRoom
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.util.matrix.getUserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import link.continuum.desktop.database.MembershipChanges
import link.continuum.desktop.events.handleInvitedRoom
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.Account
import mu.KotlinLogging

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
        membershipChanges: MembershipChanges,
        appData: AppStore
) {
    val time = data.timeline.events.lastOrNull()?.value?.origin_server_ts ?: System.currentTimeMillis()
    val roomDatas = appData.roomStore
    data.summary?.heros?.also {
        logger.info { "heros of $roomid: $it" }
        roomDatas.heroes.update(roomid, it, time)
    }
    val room = appData.roomStore.getOrCreate(roomid, account)
    val messageManager = appData.messages.get(roomid)
    withContext(UiDispatcher) {
        data.state.events.forEach { room.applyUpdate(it, appStore = appData, membershipChanges = membershipChanges) }
        val timeline = data.timeline
        timeline.events.forEach { room.applyUpdate(it.value, appStore = appData, membershipChanges = membershipChanges) }
        messageManager.appendTimeline(timeline)
    }
    room.handle_ephemeral(data.ephemeral.events.mapNotNull { it.parse() })
    // TODO:  account_data
}


@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
suspend fun processEventsResult(syncRes: SyncResponse,
                        account: Account,
                        appData: AppStore,
                        view: ChatView
                        ) {
    val self = account.userId
    syncRes.presence.events.forEach { process_presence(it) }
    // TODO: handle account_data
    val membershipChanges = MembershipChanges(appData, owner = self)
    syncRes.rooms.join.forEach{ (rid, data) ->
        val roomId = RoomId(rid)
        handle_joined_room(roomId, data, account, appData = appData, membershipChanges = membershipChanges)
    }
    syncRes.rooms.invite.forEach{ (rid, data) ->
        handleInvitedRoom(rid, data, view.invitationsView, account.server)
    }
    membershipChanges.ownerJoins(syncRes.rooms.join.keys.map { RoomId(it) })
    membershipChanges.ownerLeaves(syncRes.rooms.leave.keys)
    membershipChanges.saveData()
}
