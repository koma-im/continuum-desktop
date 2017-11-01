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
import java.util.*

/**
 * handle the checking of json dict
 */
fun RawMessage.parse(): RoomMessage {
    val message = this
    val date: Date = Date(message.origin_server_ts)
    val sender = UserStore.getOrCreateUserId(message.sender)
    val msgShow = when (message.type) {
        RoomEventType.Create -> RoomCreationMsg.fromMessage(message)
        RoomEventType.Member -> parseMemberChangeMessage(message)
        RoomEventType.Message -> parseChatMessage(sender, date, message.content)
        RoomEventType.Aliases -> parseAliasesMessage(message)
        RoomEventType.Avatar -> RoomIconUpdate(message.content["url"] as String)
        RoomEventType.CanonAlias -> RoomCanonicalAlias( RoomAlias(message.content["alias"] as String))
        RoomEventType.PowerLevels -> parsePowerLevels(message.content)
        RoomEventType.JoinRule -> RoomJoinRuleUpdate(RoomJoinRules.fromString(message.content["join_rule"] as String)!!)
        RoomEventType.HistoryVisibility -> RoomHistoryVisibilityUpdate(
                HistoryVisibility.fromString(content["history_visibility"] as String)!!)
        else -> {
            throw JsonDataException("unhandled message type ${message.type}")
        }
    }
    return msgShow
}

private fun parseAliasesMessage(message: RawMessage): RoomMessage {
    val maybealiases = message.content["aliases"]
    if (maybealiases is List<*>) {
        val aliases = maybealiases.filterIsInstance<String>().map { RoomAlias(it) }
        return RoomAliasUpdate(aliases)
    } else {
        throw JsonDataException("failed to get new aliases $message")
    }
}

private fun parsePowerLevels(content: Map<String, Any>): RoomMessage {
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
    return RoomPowerLevel(powerLevels, userLevels)
}

