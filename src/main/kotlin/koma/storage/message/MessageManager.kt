package koma.storage.message

import io.requery.Persistable
import io.requery.kotlin.asc
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import io.requery.kotlin.lte
import io.requery.sql.KotlinEntityDataStore
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.transformation.SortedList
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.RawJson
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.Timeline
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchPreceding
import koma.util.fold
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.toEventRowList
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.gui.checkUiThread
import link.continuum.desktop.gui.list.DedupList
import model.Room
import mu.KotlinLogging

import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessageManager(val room: Room,
                     private val data: KotlinEntityDataStore<Persistable>) {
    private val roomId = room.id
    // added to UI, needs to be modified on FX thread
    private val eventAll = DedupList<Pair<RoomEventRow, Room>, String>({it.first.event_id})
    val shownList = SortedList(eventAll.list) { e1, e2 ->
        e1.first.server_time.compareTo(e2.first.server_time)
    }

    private var prevLatestRow: RoomEventRow? = null

    init {
        GlobalScope.launch(Dispatchers.JavaFx) {
            showLatest()
        }
    }

    /**
     * try to load from db, and fetch from server if not available
     */
    fun getPrecedingRows(row: RoomEventRow) {
        val rows = data.select(RoomEventRow::class)
                .where(RoomEventRow::server_time.lte(row.server_time)
                        .and(RoomEventRow::room_id.eq(room.id.str)))
                .orderBy(RoomEventRow::server_time.asc()).limit(100).get().toList()
        GlobalScope.launch(Dispatchers.JavaFx) {
            addToUi(rows)
        }
    }

    private val startedFetchJobs = ConcurrentHashMap<Pair<String, FetchDirection>, SimpleBooleanProperty>()
    @ExperimentalCoroutinesApi
    fun fetchPrecedingRows(row: RoomEventRow): SimpleBooleanProperty {
        checkUiThread()
        val status = startedFetchJobs.computeIfAbsent(row.event_id to FetchDirection.Backward) {
            val loading = SimpleBooleanProperty(true)
            GlobalScope.launch {
                fetchPreceding(row).fold({
                    if (it.prevKey == row.preceding_batch) {
                        row.preceding_stored = true
                        logger.info { "reached earliest point at event $row" }
                        data.upsert(row)
                    } else {
                        val rows = it.messages.toEventRowList(roomId)
                        rows.firstOrNull()?.preceding_batch = it.prevKey
                        insert(rows, tail = row)
                    }
                    withContext(UiDispatcher) {
                        logger.debug { "loading of messages before ${row.event_id} finished" }
                        loading.set(false)
                    }
                }, {
                    logger.error { "fetch preceding events for $row ${it}" }
                })
                startedFetchJobs.remove(row.event_id to FetchDirection.Backward)
            }
            loading
        }
        return status
    }

    /**
     * load and add latest messages to ui
     */
    private suspend fun showLatest() {
        val latest = data.select(RoomEventRow::class)
                .where(RoomEventRow::room_id.eq(roomId.id))
                .orderBy(RoomEventRow::server_time.desc()).limit(100).get().reversed()
        logger.debug { "loaded latest ${latest.size} messages in $roomId" }
        addToUi(latest)
    }

    /**
     * add events that are new
     */
    private suspend fun addToUi(rows: List<RoomEventRow>) {
        val old = eventAll.size()
        withContext(Dispatchers.JavaFx) {
            eventAll.addAll(rows.map { it to room })
        }
        val newSize = eventAll.size()
        val add = newSize - old
        logger.debug { "added ${add}/${rows.size} messages." +
                " total messages $old to $newSize" }
    }

    /**
     * events to be inserted are before tail, when it's not null
     */
    private suspend fun insert(
            rows: List<RoomEventRow>,  head: RoomEventRow? = null, tail: RoomEventRow? = null
    ) {
        if (head != null) {
            rows.firstOrNull()?.preceding_stored = true
            head.following_stored = true
            data.upsert(head)
        }
        if (tail != null) {
            rows.lastOrNull()?.following_stored = true
            tail.preceding_stored = true
            data.upsert(tail)
        }
        data.upsert(rows)
        addToUi(rows)
    }

    /**
     * append messages returned by the sync api
     */
    suspend fun appendTimeline(timeline: Timeline<RawJson<RoomEvent>>) {
        val rows = timeline.events.toEventRowList(roomId)
        rows.firstOrNull()?.preceding_batch = timeline.prev_batch
        rows.firstOrNull()?.preceding_stored = timeline.limited == false
        logger.debug { "timeline with ${rows.size} messages, limited=${timeline.limited}" }

        insert(rows)

        val latest = prevLatestRow
        if (latest != null) {
            latest.following_stored = true
            data.upsert(latest)
        }
        prevLatestRow = rows.lastOrNull()
    }
}
