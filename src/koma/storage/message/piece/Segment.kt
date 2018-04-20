package koma.storage.message.piece

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEvent
import java.nio.file.Path

class Segment(
        val path: Path,
        val key: Long,
        events: List<RoomEvent>
) {
    val list: ObservableList<RoomEvent>
    init {
        if (events.size < 1) throw IllegalArgumentException("Segment should not be empty")
        this.list = FXCollections.observableArrayList(events)
    }
    /**
     * first event stored in next text file
     * used to tell whether there are gaps
     */
    var following_event: EventId? = null
    /**
     * temporary keys
     */
    var prev_batch: String? = null
    var next_batch: String? = null

    /**
     * whether work has started to fetch more messages that come earlier
     */
    var fetchEarlierStarted: Boolean = false

    var savedHash: Int? = null
    fun needSave(): Boolean = savedHash == null || this.contentHash() != savedHash

    fun contentHash(): Int = list.hashCode() * 31 + (following_event?.hashCode() ?: 0)

    override fun toString(): String {
        return "segment stored at $path"
    }

    fun isFollowedBy(s: Segment): Boolean {
        val f = following_event
        return f != null && f == s.list.firstOrNull()?.event_id
    }

    fun setFollowedBy(s: Segment) {
        following_event = s.list.firstOrNull()?.event_id
    }
}
