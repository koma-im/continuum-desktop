package model

import com.smith.faktor.UserState
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import koma.matrix.UserId
import koma_app.appState
import tornadofx.ItemViewModel
import java.text.SimpleDateFormat

data class Message(
        val age: Long,
        val event_id: String,
        val origin_server_ts: Long,
        val room_id: String,
        val type: String,
        val sender: UserId,
        val content: Map<String, Any>) {

    fun isChat(): Boolean {
        return when (this.type) {
            "m.room.create" -> true
            "m.room.member" -> true
            "m.room.message" -> true
            else -> false
        }
    }
}

class MessageItem(val msgjson: Message) {

    // these don't work if put in Message as transient fields
    val dateShown = SimpleStringProperty(SimpleDateFormat("HH:mm").format(java.util.Date(msgjson.origin_server_ts)))
    val sender= SimpleObjectProperty<UserState>(appState.getOrCreateUserId(msgjson.sender))
    val message = SimpleObjectProperty<MsgType>()

    init {
        when (msgjson.type) {
            "m.room.create" ->
                message.set(TextMsg("Room created"))
            "m.room.member" -> {
                val txt = if (msgjson.content["membership"] == "join") "Joined" else "Left"
                message.set(TextMsg(txt))
            }
            "m.room.message" -> handleMsgTypes()
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
data class ImageMsg(val desc: String, val mxcurl: String): MsgType()

fun List<Message>.filterChat(): List<Message> {
    return this
            .asSequence()
            .filter({ it.isChat() })
            .toList()
}

data class StateMessage(val age: Long,
                   val event_id: String,
                   val origin_server_ts: Long,
                   val room_id: String,
                   val type: String,
                   val sender: UserId,
                   val content: Map<String, Any>,
                   val state_key: String
) {

}
