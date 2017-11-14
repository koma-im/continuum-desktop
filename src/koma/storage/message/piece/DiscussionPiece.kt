package koma.storage.message.piece

import javafx.collections.ObservableList
import koma.matrix.event.room_message.RoomMessage

/**
 * collection of continuous messages, without gaps
 */
class DiscussionPiece(
        val messages: ObservableList<RoomMessage>,
        /**
         * used to fetch earlier messages
         */
        val batches: BatchKeys,
        /**
         * location in the merged list
         */
        var externIndex: Int,
        val neighbors: Neighbors
        )

data class BatchKeys(
        var prev: String,
        var next: String
)

data class Neighbors(
        var prev: NeighborLink?,
        var next: NeighborLink?
)

data class NeighborLink(
        var value: DiscussionPiece,
        var hasGap: Boolean = true
)
