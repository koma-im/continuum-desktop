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
    /**
     * closest pieces that are available locally
     * there can be gaps in regard to the complete timeline on the server
     */
    val neighbors= NeighborsFetched()

    /**
     * used to tell whether there are gaps
     */
    val comes = FromBatchkeys()
}

data class BatchKeys(
        var prev: String,
        var next: String
)

data class NeighborsFetched(
        var prev: DiscussionPiece?= null,
        var next: DiscussionPiece?= null
)


data class FromBatchkeys(
        /**
         * next_batch of previous piece
         */
        var after: String?= null,
        var before: String?= null
)
