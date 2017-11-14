package koma.storage.message

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.parse
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.pagination.FetchDirection
import koma.storage.message.fetch.LoadRoomMessagesService
import koma.storage.message.piece.BatchKeys
import koma.storage.message.piece.DiscussionPiece
import matrix.room.Timeline

class MessageManager(val roomid: String) {
    private val pieces = mutableListOf<DiscussionPiece>()

    /**
     * merged list shown to the user
     */
    private val _messages: ObservableList<RoomMessage> = FXCollections.observableArrayList<RoomMessage>()
    val messages = FXCollections.unmodifiableObservableList(_messages)

    fun appendTimeline(timeline: Timeline<RoomMessage>, next_batch: String) {
        synchronized(pieces) {
            if (pieces.size != 0 && timeline.limited != true) {
                val last = pieces.last()
                extendPieceForward(last, timeline.events, next_batch)
            } else {
                val last = pieces.lastOrNull()
                val range_beg = last?.let { it.externIndex + it.messages.size } ?: 0
                val p = DiscussionPiece(
                        timeline.events.toMutableList(),
                        BatchKeys(prev = timeline.prev_batch!!, next = next_batch),
                        range_beg
                )
                val previousFetchProgress = last?.batches?.next
                pieces.add(p)
                _messages.addAll(range_beg, timeline.events)
                last?.let {
                    it.neighbors.next.value = p
                    p.neighbors.prev.value = it
                }
                fetchEarlier(p, previousFetchProgress)
            }
        }
    }

    /**
     * toward the past
     * use synchronized to be safe
     * TODO deduplication may be needed
     */
    private fun extendPieceBackward(piece: DiscussionPiece, messages: List<RoomMessage>, newpoint: String) {
        synchronized(pieces) {
            piece.batches.prev = newpoint
            piece.messages.addAll(0, messages)
            this._messages.addAll(piece.externIndex, messages)
            shiftForward(piece, messages.size)
        }
    }

    /**
     * toward the future
     */
    private fun extendPieceForward(p: DiscussionPiece, messages: List<RoomMessage>, newend: String) {
        synchronized(pieces) {
            p.batches.next = newend
            p.messages.addAll(messages)
            this._messages.addAll(p.externIndex + messages.size, messages)
            shiftForward(p, messages.size)
        }
    }

    private fun shiftForward(piece: DiscussionPiece, len: Int) {
        var movedpiece = piece.neighbors.next?.value
        while (movedpiece != null) {
            movedpiece.externIndex += len
            movedpiece = movedpiece.neighbors.next?.value
        }
    }

    fun fetchEarlier(piece: DiscussionPiece, limit_key: String? = null) {
        val historyNeeded = 200
        var historyFetched = 0
        val serv = LoadRoomMessagesService(roomid, piece.batches.prev, FetchDirection.Backward, limit_key)
        serv.setOnSucceeded {
            val prev_chunk = serv.value
            if (prev_chunk != null) {
                val parsed_events = prev_chunk.chunk
                        .map { it.toMessage().parse() }
                        .asReversed()
                historyFetched += parsed_events.size
                if (historyFetched > historyNeeded) {
                    serv.cancel()
                }
                extendPieceBackward(piece, parsed_events, prev_chunk.end)
            }
        }
        serv.start()
    }
}
