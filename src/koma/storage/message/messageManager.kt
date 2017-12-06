package koma.storage.message

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchEarlier
import koma.storage.message.piece.DiscussionPiece
import koma.storage.message.piece.Stitcher
import koma.storage.message.piece.loadStoredDiscussion
import koma.storage.message.piece.set_log_path
import kotlinx.coroutines.experimental.launch
import matrix.room.Timeline

class MessageManager(val roomid: RoomId) {
    val stitcher: Stitcher
    /**
     * merged list shown to the user
     */
    val messages: ObservableList<RoomMessage>

    init {
        val _messages: ObservableList<RoomMessage> = FXCollections.observableArrayList<RoomMessage>()
        stitcher = Stitcher(_messages)
        messages = FXCollections.unmodifiableObservableList(_messages)

        val stored = loadStoredDiscussion(roomid)
        stored.forEach { this.stitcher.insertPiece(it) }
    }



    fun appendTimeline(timeline: Timeline<RoomMessage>, continued: Boolean) {
        val time = timeline.events.firstOrNull()?.original?.origin_server_ts
        time?: return
        synchronized(stitcher) {
            if (!continued
                    ||timeline.limited == true
               || !this.stitcher.insertIntoLast(timeline.events)) {

                val p = DiscussionPiece(
                        timeline.events.toMutableList(),
                        time
                )
                p.prev_batch = timeline.prev_batch
                p.set_log_path(time, roomid)
                this.stitcher.insertPiece(p)

                launch {
                    val last = this@MessageManager.stitcher.lastPiece()
                    last?.let {  fetchEarlier(it)}
                }
            }
        }
    }


}
