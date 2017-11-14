package koma.matrix.event

import com.squareup.moshi.JsonDataException
import koma.matrix.event.room_message.*
import koma.matrix.event.room_message.chat.parseChatMessage
import koma.matrix.event.room_message.member.parseMemberChangeMessage
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.sync.RawMessage
import koma.storage.users.UserStore
import matrix.event.room_message.RoomEventType

/**
 * handle the checking of json dict
 */
fun RawMessage.parse(): RoomMessage {
    val message = this
    val time = message.origin_server_ts
    val sender = UserStore.getOrCreateUserId(message.sender)
    val msgShow = when (message.type) {
        RoomEventType.Create -> RoomCreationMsg.fromMessage(message)
        RoomEventType.Member -> parseMemberChangeMessage(message)
        RoomEventType.Message -> parseChatMessage(sender, time, message.content)
        RoomEventType.Aliases -> parseAliasesMessage(message)
        RoomEventType.Avatar -> RoomIconUpdate(time, message.content["url"] as String)
        RoomEventType.CanonAlias -> RoomCanonicalAlias(time, RoomAlias(message.content["alias"] as String))
        RoomEventType.PowerLevels -> parsePowerLevels(message)
        RoomEventType.JoinRule -> RoomJoinRuleUpdate(time, RoomJoinRules.fromString(message.content["join_rule"] as String)!!)
        RoomEventType.HistoryVisibility -> RoomHistoryVisibilityUpdate(time,
                HistoryVisibility.fromString(content["history_visibility"] as String)!!)
        RoomEventType.Name -> RoomNameUpdate(time, content["name"].toString())
        RoomEventType.GuestAccess -> RoomGuestAccess(time, content)
        RoomEventType.Topic -> RoomTopic(time, content)
        RoomEventType.Redaction -> RoomRedaction(time, content)
    }
    return msgShow
}

private fun parseAliasesMessage(message: RawMessage): RoomMessage {
    val maybealiases = message.content["aliases"]
    if (maybealiases is List<*>) {
        val aliases = maybealiases.filterIsInstance<String>().map { RoomAlias(it) }
        return RoomAliasUpdate(message.origin_server_ts, aliases)
    } else {
        throw JsonDataException("failed to get new aliases $message")
    }
}

private fun parsePowerLevels(message: RawMessage): RoomMessage {
    val content = message.content
    val powerLevels = mutableMapOf<String, Int>()
    val userLevels = mutableMapOf<String, Double>()

    val map = content.toMutableMap()
    val events = map.remove("events")
    if (events != null ) {
        powerLevels.putAll(events as Map<String, Int>)
    }
    val users = map.remove("users")
    if (users != null ) {
        userLevels.putAll(users as Map<String, Double>)
    }
    powerLevels.putAll(map.toMap() as Map<String, Int>)
    return RoomPowerLevel(message.origin_server_ts, powerLevels, userLevels)
}

