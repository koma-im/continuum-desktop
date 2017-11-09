package koma.storage.message

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.parse
import koma.matrix.event.room_message.ChatMessage
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.pagination.FetchDirection
import koma.storage.message.fetch.LoadRoomMessagesService
import matrix.room.Timeline

class MessageManager(val roomid: String) {
    val messages: ObservableList<RoomMessage> = FXCollections.observableArrayList<RoomMessage>()

    fun prependMessages(events: List<RoomMessage>) {
        messages.addAll(0, events)
    }

    fun processNormalMessage(message: ChatMessage) {
        messages.add(message)
    }
}

/**
 * handle new messages from the sync api
 * when the timeline is not complete, start a service to fetch previous messages
 */
fun MessageManager.appendTimeline(timeline: Timeline<RoomMessage>) {
    messages.addAll(timeline.events)
    val historyNeeded = 200
    var historyFetched = 0
    if (timeline.limited == true && timeline.prev_batch != null) {
        val serv = LoadRoomMessagesService(roomid, timeline.prev_batch, FetchDirection.Backward)
        serv.setOnSucceeded {
            val prev_events = serv.value
            if (prev_events != null) {
                val parsed_events = prev_events
                        .map { it.toMessage().parse() }
                        .asReversed()
                historyFetched += parsed_events.size
                if (historyFetched > historyNeeded) {
                    serv.cancel()
                }
                this.prependMessages(parsed_events)
            }
        }
        serv.start()
    }
}
