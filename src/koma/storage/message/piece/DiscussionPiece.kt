package koma.storage.message.piece

import koma.matrix.event.room_message.RoomEvent

/**
 * collection of continuous messages, without gaps
 */
class DiscussionPiece(
        /**
         * must be a sorted list
         */
        val messages: MutableList<RoomEvent>,
        /**
         * used to sort by time
         * time of some message contained within
         */
        val timekey: Long
): OrderedListPart<Long, RoomEvent> {
    /**
     * temporary keys
     */
    var prev_batch: String? = null
    var next_batch: String? = null

    /**
     * first event stored in next text file
     * used to tell whether there are gaps
     */
    var following_event: String? = null

    var filename: String? = null

    var savedHash: Int? = null
    fun needSave(): Boolean = savedHash == null || this.contentHash() != savedHash

    fun contentHash(): Int = messages.hashCode() * 31 + (following_event?.hashCode() ?: 0)

    override fun getList(): MutableList<RoomEvent> {
        return this.messages
    }

    override fun getKey(): Long = this.timekey

    init {

        Runtime.getRuntime().addShutdownHook(Thread({
            this.save()
        }))
    }

    override fun toString(): String {
        return "<messages from ${this.timekey}: ... ${this.messages.lastOrNull()}>"
    }
}

interface OrderedListPart<K, E>{
    fun getList(): MutableList<E>
    fun getKey(): K
}


