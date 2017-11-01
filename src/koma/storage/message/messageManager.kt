package koma.storage.message

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.room_message.ChatMessage
import koma.matrix.event.room_message.RoomMessage

class MessageManager(val key: String) {
    val messages: ObservableList<RoomMessage> = FXCollections.observableArrayList<RoomMessage>()

    fun prependMessages(events: List<RoomMessage>) {
        messages.addAll(0, events)
    }

    fun processNormalMessage(message: ChatMessage) {
        messages.add(message)
    }

    fun appendMessages(events: List<RoomMessage>) {
        messages.addAll( events)
    }

}
