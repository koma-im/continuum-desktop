package koma.storage.message

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchEarlier
import koma.storage.message.piece.DiscussionPiece
import koma.storage.message.piece.Stitcher
import koma.storage.message.piece.loadStoredDiscussion
import koma.storage.message.piece.set_log_path
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import matrix.room.Timeline

class MessageManager(val roomid: RoomId) {
    val stitcher: Stitcher
    /**
     * merged list shown to the user
     */
    val messages: ObservableList<RoomEvent>

    var continued = false

    init {
        println("Loading messages of room $roomid")
        val _messages: ObservableList<RoomEvent> = FXCollections.observableArrayList<RoomEvent>()
        stitcher = Stitcher(_messages)
        messages = FXCollections.unmodifiableObservableList(_messages)

        val stored = loadStoredDiscussion(roomid)
        stored.filter { it.getList().isNotEmpty() }.forEach { this.stitcher.insertPiece(it) }
    }



    fun appendTimeline(timeline: Timeline<RoomEvent>) {
        val time = timeline.events.firstOrNull()?.origin_server_ts
        time?: return
        synchronized(stitcher) {
            if (!continued
                    ||timeline.limited == true
               || !this.stitcher.insertIntoLast(timeline.events)) {

                continued = true
                val p = DiscussionPiece(
                        timeline.events.toMutableList(),
                        time
                )
                p.prev_batch = timeline.prev_batch
                if (this.stitcher.insertPiece(p)) {
                    p.set_log_path(time, roomid)
                }

                launch(JavaFx) {
                    val last = this@MessageManager.stitcher.lastPiece()
                    last?.let {  fetchEarlier(it)}
                }
            }
        }
    }


}
