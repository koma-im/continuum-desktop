package koma.matrix.event.room_message.member

import com.google.gson.JsonParseException
import koma.matrix.event.room_message.*
import koma.matrix.sync.RawMessage
import koma.storage.users.UserStore
import java.util.*

/**
 * actually, the param type and return type should be more specific
 * but i don't know how to express it nicely in kotlin
 */
fun parseMemberChangeMessage(message: RawMessage): RoomMessage {

    val membership = message.content["membership"]
    val sender = UserStore.getOrCreateUserId(message.sender)
    val datetime: Date = Date(message.origin_server_ts)

    if (membership == "join") {
        // join is used for both actual joining and updating user info
        val avatar_new = message.content.get("avatar_url") as String?
        val name_new = message.content.get("displayname") as String?

        // updating user info while parsing doesn't feel right, how to improve?
        avatar_new?.let { sender.avatarURL.set(it) }
        name_new?.let { sender.displayName.set(it) }

        val notnew = message.prev_content?.get("membership") == "join"
        if (notnew) {
            val avatar_old = message.prev_content?.get("avatar_url") as String?
            val name_old = message.prev_content?.get("displayname") as String?

            return MemberUpdateMsg(sender, datetime,
                    Pair(name_old, name_new), Pair(avatar_old, avatar_new))
        } else {
            return MemberJoinMsg(sender, datetime, name_new, avatar_new)
        }
    } else if (membership == "leave") {
        return MemberLeave(sender, datetime)
    } else if (membership == "ban") {
        return MemberBan(sender, datetime)
    } else if (membership == "invite") {
        return MemberJoin(sender, datetime)
    } else
        throw JsonParseException("Unexpected membership change: $message")
}
