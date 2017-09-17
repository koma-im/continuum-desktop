package model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.users.UserStore
import matrix.event.roomevent.RoomEventType
import tornadofx.*
import java.text.SimpleDateFormat

data class Message(
        val age: Long?,
        val event_id: String,
        val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val type: RoomEventType,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Map<String, Any>) {

    fun isChat(): Boolean {
        return when (this.type) {
            RoomEventType.Create -> true
            RoomEventType.Member -> true
            RoomEventType.Message -> true
            else -> false
        }
    }
}

class MessageItem(val msgjson: Message) {

    // these don't work if put in Message as transient fields
    val dateShown = SimpleStringProperty(SimpleDateFormat("HH:mm").format(java.util.Date(msgjson.origin_server_ts)))
    val sender= SimpleObjectProperty<UserState>(UserStore.getOrCreateUserId(msgjson.sender))
    val message = SimpleObjectProperty<MsgType>()

    init {
        when (msgjson.type) {
            RoomEventType.Create ->
                message.set(TextMsg("Room created"))
            RoomEventType.Member -> {
                val txt = if (msgjson.content["membership"] == "join") "Joined" else "Left"
                message.set(TextMsg(txt))
            }
            RoomEventType.Message -> handleMsgTypes()
            else -> {
                val text = "Unhandled message type: ${msgjson.type}"
                message.set(TextMsg(text))
            }
        }
    }

    private fun handleMsgTypes() {
        val msgtype = msgjson.content["msgtype"] as String?
        if (msgtype == null) {
            message.set(TextMsg("(unexpected unknown message type)"))
            return
        }
        val msg = when (msgtype) {
            "m.text" -> TextMsg(msgjson.content["body"] as String? ?: "(unexpected empty text)")
            "m.emote" -> EmoteMsg(msgjson.content["body"] as String? ?: "")
            "m.image" -> ImageMsg(desc = msgjson.content["body"] as String? ?: "",
                    mxcurl = msgjson.content["url"] as String? ?: "")
            else -> TextMsg("(unexpected other message type)")
        }
        message.set(msg)
    }
}

class MessageItemModel(property: ObjectProperty<MessageItem>) : ItemViewModel<MessageItem>(itemProperty = property) {
    val date = bind { item?.dateShown }
    val sender = bind { item?.sender}
    val message = bind {item?.message}
}

sealed class MsgType

data class TextMsg(val text: String): MsgType()
data class EmoteMsg(val text: String): MsgType()
data class ImageMsg(val desc: String, val mxcurl: String): MsgType()

fun List<Message>.filterChat(): List<Message> {
    return this
            .asSequence()
            .filter({ it.isChat() })
            .toList()
}

