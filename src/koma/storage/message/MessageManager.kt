package koma.storage.message

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import koma.gui.element.control.NullableIndexRange
import koma.gui.view.window.chatroom.messaging.reading.display.supportedByDisplay
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchEarlier
import koma.storage.message.file.SegmentsDirectory
import koma.storage.message.file.get_log_path
import koma.storage.message.piece.Segment
import koma.util.observable.list.concat.TreeConcatList
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import matrix.room.Timeline
import tornadofx.*
import kotlinx.coroutines.experimental.launch as corolaunch

class MessageManager(val roomId: RoomId) {
    // added to UI, needs to be modified on FX thread
    private val concatList = TreeConcatList<Long, RoomEvent>()
    val shownList = FilteredList(concatList, { it.supportedByDisplay() })
    private val segDir = SegmentsDirectory(roomId)
    // points to the segment containing newest messages being received
    private var current: Long? = null

    private val visibleRange = SimpleObjectProperty<NullableIndexRange>()
    /**
     * key to earliest one shown on screen
     */
    private val visibleKey = objectBinding(visibleRange) {
        value?.start?.let { concatList.locateKey(it) }
    }
    private val visibleLast = longBinding(visibleRange) {
        value?.endInclusive?.let { concatList.locateKey(it)?.key } ?: -1
    }

    val chan = actor<MessageManagerMsg> {
        for (msg in channel) {
            when (msg) {
                is AppendSync -> appendTimeline(msg.timeline)
                is ShowLatest -> showLatest()
                is VisibleRange -> { visibleRange.cleanBind(msg.property) }
                is StartFetchEarlier -> startFetchEarlierOnce(msg.index)
                is PrependFetched -> {
                    val ns = prependFetched(msg.key, msg.messages)
                    val ne = if(ns.fetchEarlierStarted) null else ns
                    msg.newEdge.complete(ne)
                }
                is FillUi -> fillViewBefore(msg.key)
            }
        }
    }

    init {
        visibleLast.onChange {
            if (it > 42) {
                corolaunch {
                    chan.send(FillUi(it))
                }
            }
        }
    }

    private fun startFetchEarlierOnce(key: Long) {
        val s = segDir.get(key)
        s?:return
        if (!s.fetchEarlierStarted) {
            s.fetchEarlierStarted = true
            val sl = findLowEnd(s.key)
            if (sl.key == s.key) {
                corolaunch {
                    fetchEarlier(chan, s, visibleKey, roomId)
                }
            } else if (!sl.fetchEarlierStarted) {
                sl.fetchEarlierStarted = true
                corolaunch {
                    fetchEarlier(chan, sl, visibleKey, roomId)
                }
            }
        }
    }
    private suspend fun fillViewBefore(key: Long) {
        var loaded = 0
        var lower = this.segDir.lower(key, false)
        while (loaded < 100 && lower != null) {
            addToUi(lower.key, lower.list)
            loaded += lower.list.size
            lower = this.segDir.lower(lower.key, false)
        }
    }
    /**
     * load and add latest messages to ui
     */
    private suspend fun showLatest() {
        val last = this.segDir.last()
        last ?: return
        addToUi(last.key, last.list)
        fillViewBefore(last.key)
        corolaunch { chan.send(StartFetchEarlier(last.key)) }
    }
    private suspend fun addToUi(key: Long, list: ObservableList<RoomEvent>) {
        if (this.concatList.containsKey(key)) return
        withContext(JavaFx) {
            this.concatList.put(key, list)
        }
    }
    /**
     * returns the key to the segment that actually got new elements
     */
    private fun locateInsert(elements: List<RoomEvent>): Long? {
        val first = elements.first().origin_server_ts
        val last = elements.last().origin_server_ts
        var gap: Gap? = null
        for (g in GapIterator(last)) {
            val rel = g.locate(first, last)
            if (rel == Gap.Relation.TooHigh) {
                break
            } else if (rel == Gap.Relation.Inter) {
                gap = g
                break
            }
        }
        println("[$first, $last] has been located in $gap")
        gap ?: return null
        val trimlow = gap.trimLow(elements)
        val leftTrimmed = trimlow.size < elements.size
        val newElements = gap.trimHigh(trimlow)
        val rightTrimmed = newElements.size < trimlow.size
        if (newElements.size == 0) {
            return null
        }
        val key = newElements.first().origin_server_ts
        val path = get_log_path(key, roomId)
        path ?: return null
        val newsegment = Segment(path, key, newElements)
        if (leftTrimmed) gap.low?.setFollowedBy(newsegment)
        if (rightTrimmed) gap.high?.let { newsegment.setFollowedBy(it) }
        this.segDir.put(key, newsegment)

        return key
    }

    /**
     * append messages returned by the sync api
     */
    private suspend fun appendTimeline(timeline: Timeline<RoomEvent>) {
        val first = timeline.events.firstOrNull()
        first ?: return
        val cur = current
        if (cur!=null && timeline.limited == false) {
            appendTo(cur, timeline.events)
            return
        } else {
            val newk = locateInsert(timeline.events)
            if (newk != null) {
                val new = this.get(newk)!!
                if (new.list.firstOrNull() == first) {
                    new.prev_batch = timeline.prev_batch
                }
                addToUi(newk, new.list)
                chan.send(StartFetchEarlier(newk))
            }
            current = newk
        }
    }


    private suspend fun prependFetched(key: Long, elements: List<RoomEvent>): Segment {
        return if (prependTo(key, elements)) {
            findLowEnd(key)
        } else {
            val s = this.segDir.get(key)!!
            s
        }
    }
    /**
     * returns whether it is united with the neighboring segment
     */
    private suspend fun prependTo(key: Long, elements: List<RoomEvent>): Boolean {
        val n_all = elements.size
        val newelements = filterPre(key, elements)
        val p = this.segDir[key]!!
        withContext(if (concatList.containsKey(key)) JavaFx else DefaultDispatcher) {
            p.list.addAll(0, newelements)
        }
        return newelements.size < n_all
    }

    private fun appendTo(key: Long, elements: List<RoomEvent>): Boolean {
        val n_req = elements.size
        val newelements = filterNext(key, elements)
        val p = this.segDir[key]!!
        p.list.addAll(newelements)
        return newelements.size < n_req
    }

    /**
     * newest first
     */
    private inner class GapIterator(start: Long): Iterator<Gap> {
        private var nextGap: Gap?
        init {
            val highBound = this@MessageManager.segDir.higher(start, true)?.let {
                this@MessageManager.findLowEnd(it.key)
            }
            val lowstart = highBound?.key ?: start
            val lowBound = this@MessageManager.segDir.lower(lowstart, false)
            nextGap = Gap(lowBound, highBound)
        }
        override fun hasNext(): Boolean = nextGap != null
        override fun next(): Gap {
            val g = nextGap!!
            nextGap = findNext(g.low)
            return g
        }
        private fun findNext(start: Segment?): Gap? {
            start ?: return null
            val high = this@MessageManager.findLowEnd(start.key)
            val lowBound = this@MessageManager.segDir.lower(high.key, false)
            return Gap(lowBound, high)
        }
    }
    /**
     * where there are usually messages not yet stored
     */
    private class Gap(
            val low: Segment?,
            val high: Segment?
    ) {
        enum class Relation{
            TooLow,
            Inter,
            TooHigh
        }
        /**
         * find out whether a range may intersect with this
         */
        fun locate(first: Long, last: Long): Relation {
            return if (low != null && last < low.list.first().origin_server_ts)
                Relation.TooLow
            else if (high != null && first > high.list.last().origin_server_ts)
                Relation.TooHigh
            else
                Relation.Inter
        }

        override fun toString(): String {
            return "(${low?.list?.lastOrNull()?.origin_server_ts},${high?.list?.firstOrNull()?.origin_server_ts})"
        }

        fun trimLow(elements: List<RoomEvent>): List<RoomEvent> {
            val prev = low?.list
            if (prev != null && prev.last() >= elements.first() ) {
                return elements.filter {
                    if (it < prev.last()) false
                    else if (it > prev.last()) true
                    else !prev.contains(it)
                }
            }
            return  elements
        }

        fun trimHigh(elements: List<RoomEvent>): List<RoomEvent> {
            val next = high?.list
            return if (next != null && next.first() <= elements.last() ) {
                elements.filter {
                    if (it < next.first()) true
                    else if (it > next.first()) false
                    else !next.contains(it)
                }
            } else elements
        }
    }

    private fun filterPre(key: Long, elements: List<RoomEvent>): List<RoomEvent> {
        val prev = this.lowerList(key)
        if (prev != null && prev.last() >= elements.first() ) {
            return elements.filter {
                if (it < prev.last()) false
                else if (it > prev.last()) true
                else !prev.contains(it)
            }
        }
        return  elements
    }

    private fun filterNext(key: Long, elements: List<RoomEvent>): List<RoomEvent> {
        val next = this.higherList(key)
        return if (next != null && next.first() <= elements.last() ) {
            elements.filter {
                if (it < next.first()) true
                else if (it > next.first()) false
                else !next.contains(it)
            }
        } else elements
    }

    private fun lowerList(key: Long): ObservableList<RoomEvent>? {
        return this.segDir.lower(key, false)?.list
    }
    private fun higherList(key: Long): ObservableList<RoomEvent>? {
        return this.segDir.higher(key, false)?.list
    }
    /**
     * skip segments that are connected and find one that has a gap below it
     */
    private fun findLowEnd(key: Long): Segment {
        var cur = this.segDir[key]!!
        var low = this.segDir.lower(key, false)
        while (low != null && low.isFollowedBy(cur)) {
            cur = low
            low = this.segDir.lower(cur.key, false)
        }
        return cur
    }
    private fun get(key: Long): Segment? {
        return this.segDir[key]
    }
}
