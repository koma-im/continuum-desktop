package koma.storage.message.piece

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.MoshiInstance
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

    var meta = Metadata(null, null, null)
    data class Metadata(
            /**
             * first event stored in next text file
             * used to tell whether there are gaps
             */
            var following_event: EventId? = null,
            /**
             * temporary keys
             */
            var prev_batch: String? = null,
            var next_batch: String? = null
    ) {
        companion object {
            val adapter = MoshiInstance.moshi.adapter(Metadata::class.java)
        }
    }

    /**
     * whether work has started to fetch more messages that come earlier
     */
    var fetchEarlierStarted: Boolean = false

    var savedHash: Int? = null
    fun needSave(): Boolean = savedHash == null || this.hashCode() != savedHash

    override fun toString(): String {
        return "segment stored at $path"
    }

    fun isFollowedBy(s: Segment): Boolean {
        val f = meta.following_event
        return f != null && f == s.list.firstOrNull()?.event_id
    }

    fun setFollowedBy(s: Segment) {
        meta.following_event = s.list.firstOrNull()?.event_id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Segment

        if (list != other.list) return false
        if (meta != other.meta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = list.hashCode()
        result = 31 * result + (meta.hashCode() ?: 0)
        return result
    }
}

