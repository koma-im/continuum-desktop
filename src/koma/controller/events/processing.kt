package koma.controller.events_processing

import koma.concurrency.LoadRoomMessagesService
import koma.matrix.epemeral.GeneralEvent
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.sync.SyncResponse
import koma.matrix.user.presence.PresenceMessage
import koma.storage.rooms.RoomStore
import koma.storage.rooms.UserRoomStore
import koma.storage.users.UserStore
import koma_app.appState.apiClient
import koma_app.appState.sortMembersInEachRoom
import matrix.event.roomevent.RoomEventType
import matrix.room.InvitedRoom
import matrix.room.JoinedRoom
import model.Message
import model.MessageFromOthers
import model.Room
import model.RoomJoinRules

fun process_typing_event(msg: GeneralEvent) {
    val userIds = msg.content["user_ids"]
    if (userIds != null && userIds is List<*>) {
        val usersTyping: List<String> = userIds.map { it.toString() }
        usersTyping
                .map { UserStore.getOrCreateUser(it) }
                .filterNotNull()
                .map { it.typing.set(true) }
    }
}

fun process_presence(message: PresenceMessage) {
    message.getUserState()?.let {
        it.present.set(message.content.presence)
        val laa = message.content.last_active_ago
        if (laa is Number)
            it.lastActiveAgo.set(laa.toLong())
    }
}

fun processNormalMessage(knownRoom: Room, message: Message) {
    val msg = MessageFromOthers.fromMessage(message)
    if (msg!= null)
        knownRoom.chatMessages.add(msg)
}

fun processMemberJoinMessage(room: Room, message: Message) {
    val user = UserStore.getOrCreateUserId(message.sender)
    message.content["displayname"]?.let { user.displayName.set(it.toString()) }
    val avatarURL = message.content["avatar_url"] as String?

    if (avatarURL != null) {
        user.avatarURL.set(avatarURL)
    }
    room.makeUserJoined(user)
}

fun processMemberLeaveMessage(room: Room, message: Message) {
    room.removeMember(message.sender)
    if (apiClient?.userId == message.sender) {
        UserRoomStore.remove(room.id)
    }

}

fun processMembershipMessage(room: Room, message: Message) {

    val membership = message.content["membership"] ?: ""
    when (membership) {
        "join" -> processMemberJoinMessage(room, message)
        "leave" -> processMemberLeaveMessage(room, message)
        "ban" -> room.removeMember(message.sender)
        else -> println("Unexpected membership change: $message")
    }
}
fun handleAliasesMessage(room: Room, message: Message) {
    val maybealiases = message.content["aliases"]
    if (maybealiases is List<*>) {
        val aliases = maybealiases.filterIsInstance<String>().map { RoomAlias(it) }
        room.aliases.addAll(aliases)
    }
}

fun handleAvatarMessage(room: Room, message: Message) {
    val url = message.content["url"] as String?
    if (url != null)
        room.iconURL.set(url)
}

private fun handleCanonicalAlias(room: Room, message: Message) {
    val alias = message.content["alias"] as String?
    if (alias != null)
        room.setCanonicalAlias(RoomAlias(alias))
}

private fun handlePowerLevels(room: Room, content: Map<String, Any>) {
    val map = content.toMutableMap()
    val events = map.remove("events")
    if (events != null ) {
        room.updatePowerLevels(events as Map<String, Int>)
    }
    val users = map.remove("users")
    if (users != null ) {
        room.updateUserLevels(users as Map<String, Int>)
    }
    room.updatePowerLevels(map.toMap() as Map<String, Int>)
}
private fun handle_join_rules(room: Room, rule: Map<String, Any>) {
    val join = when (rule["join_rule"]) {
        "public" -> RoomJoinRules.Public
        "invite" -> RoomJoinRules.Invite
        // not used on the matrix network for now
        "knock" -> RoomJoinRules.Knock
        "private" -> RoomJoinRules.Private
        else -> null
    }
    if (join != null) {
        room.joinRule = join
    } else {
        println("unknown join rule for room $room: $rule")
    }
}

private fun update_history_visibility(room: Room, content: Map<String, Any>) {
    val vis = HistoryVisibility.fromString(content["history_visibility"] as String)
    if (vis != null) {
        room.histVisibility = vis
    } else {
        println("unknown history visibility for room $room: $content")
    }
}

private fun handle_room_events(room: Room, events: List<Message>) {
    events.forEach { message ->
        when (message.type) {
            RoomEventType.Create -> {} // already handled when processing keys of the map
            RoomEventType.Message -> processNormalMessage(room, message)
            RoomEventType.Member -> processMembershipMessage(room, message)
            RoomEventType.Aliases -> handleAliasesMessage(room, message)
            RoomEventType.Avatar -> handleAvatarMessage(room, message)
            RoomEventType.CanonAlias -> handleCanonicalAlias(room, message)
            RoomEventType.PowerLevels -> handlePowerLevels(room, message.content)
            RoomEventType.JoinRule -> handle_join_rules(room, message.content)
            RoomEventType.HistoryVisibility -> update_history_visibility(room, message.content)
            else -> {
                println("Unhandled message: $message")
            }
        }
    }
}
private fun handle_room_ephemeral(room: Room, events: List<GeneralEvent>) {
    events.forEach { message ->
        when (message.type) {
            "m.typing" -> process_typing_event(message)
            else -> println("Unhandled ephemeral message: " + message)
        }
    }
}

private fun handle_joined_room(roomid: String, data: JoinedRoom) {
    val room = UserRoomStore.add(roomid)
    if (data.timeline.limited == true && data.timeline.prev_batch != null) {
        val serv = LoadRoomMessagesService(roomid, data.timeline.prev_batch, FetchDirection.Backward)
        serv.setOnSucceeded {
            val prev_events = serv.value
            if (prev_events != null) {
                room.chatMessages.addAll(0,
                        prev_events
                                .asReversed()
                                .map { MessageFromOthers.fromMessage(it.toMessage()) }
                                .filterNotNull()
                )
            }
        }
        serv.start()
    }
    handle_room_events(room, data.state.events)
    handle_room_events(room, data.timeline.events)
    handle_room_ephemeral(room, data.ephemeral.events)
    // TODO:  account_data
}

private fun handle_invited_room(roomid: String, data: InvitedRoom) {
    val room = RoomStore.getOrCreate(roomid)
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
