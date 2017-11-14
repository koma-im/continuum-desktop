package koma.storage.message.piece

import koma.matrix.event.room_message.RoomMessage

/**
 * collection of continuous messages, without gaps
 */
class DiscussionPiece(
        val messages: MutableList<RoomMessage>,
        /**
         * used to fetch earlier messages
         */
        val batches: BatchKeys,
        /**
         * location in the merged list
         */
        var externIndex: Int
) {
    val neighbors= Neighbors()
}

data class BatchKeys(
        var prev: String,
        var next: String
)

data class Neighbors(
        val prev: NeighborLink= NeighborLink(),
        val next: NeighborLink= NeighborLink()
)

data class NeighborLink(
        var value: DiscussionPiece? = null,
        var hasGap: Boolean = true
)
