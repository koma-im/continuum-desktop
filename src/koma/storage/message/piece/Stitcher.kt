package koma.storage.message.piece

import koma.matrix.event.room_message.RoomMessage

class Stitcher(list: MutableList<RoomMessage>)
    : Concatenater<Long, RoomMessage, DiscussionPiece>(list) {

    init {
    }

    /**
     * toward the past
     */
    @Synchronized
    fun extendHead(key: Long, fetchedmessages: List<RoomMessage>): DiscussionPiece{
        val messages = fetchedmessages.toMutableList()

        val prev = this.lowerEntryValue(key)
        val merged = if (prev != null) {
            val pl = prev.getList()
            val prevlast = pl.last()
            val overlap = (prevlast >= messages.first()
                    && messages.any { it <= prevlast && pl.contains(it) })
            if (overlap) {
                messages.removeAll {
                    if (it < prevlast) true
                    else if (it > prevlast) false
                    else pl.contains(it)
                }
            }
            overlap
        } else false

        insertAllIntoAt(key, 0, messages)

        val cur = this.pieces.get(key)!!.item
        if (merged) {
            val pre = prev!!
            connect_together(pre, cur)
            return goBackConsecutive(pre)
        } else {
            return cur
        }
    }

    private fun goBackConsecutive(piece: DiscussionPiece): DiscussionPiece {
        var cur = piece
        for (e in this.pieces.headMap(piece.getKey(), false).descendingMap()) {
            val i = e.value.item
            if (i.following_event == cur.first_event_id()) {
                cur = i
            } else {
                break
            }
        }
        return cur
    }

    private fun connect_together(former: DiscussionPiece, Latter: DiscussionPiece) {
        former.following_event = Latter.first_event_id()
    }

    fun lastPiece(): DiscussionPiece? {
        return this.pieces.lastEntry()?.value?.item
    }
}
