package koma.storage.message

import io.requery.Persistable
import io.requery.kotlin.asc
import io.requery.kotlin.desc
import io.requery.kotlin.lte
import io.requery.sql.KotlinEntityDataStore
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import koma.gui.view.window.chatroom.messaging.reading.display.supportedByDisplay
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.Timeline
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchPreceding
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.database.models.RoomEventRow
import link.continuum.desktop.database.models.getEvent
import link.continuum.desktop.database.models.toEventRowList
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessageManager(val roomId: RoomId,
                     private val data: KotlinEntityDataStore<Persistable>) {
    // added to UI, needs to be modified on FX thread
    private val eventAll = FXCollections.observableArrayList<RoomEventRow>()
    private val eventVisible = FilteredList(eventAll) { it.getEvent()?.supportedByDisplay() == true }
    val shownList = SortedList(eventVisible)

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
                .where(RoomEventRow::server_time.lte(row.server_time))
                .orderBy(RoomEventRow::server_time.asc()).limit(100).get().toList()
        GlobalScope.launch(Dispatchers.JavaFx) {
            addToUi(rows)
        }
    }

    private val startedFetchJobs = ConcurrentHashMap<Pair<String, FetchDirection>, Job>()
    @ExperimentalCoroutinesApi
    private fun fetchPrecedingRows(row: RoomEventRow) {
        startedFetchJobs.computeIfAbsent(row.event_id to FetchDirection.Backward) {
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
                }, {
                    logger.error { "fetch preceding events for $row ${it}" }
                })
                startedFetchJobs.remove(row.event_id to FetchDirection.Backward)
            }
        }
    }

    /**
     * load and add latest messages to ui
     */
    private suspend fun showLatest() {
        val latest = data.select(RoomEventRow::class).orderBy(RoomEventRow::server_time.desc()).limit(100).get().reversed()
        addToUi(latest)
    }

    /**
     * add events that are new
     */
    private suspend fun addToUi(rows: List<RoomEventRow>) {
        val exist = this.eventAll.map { it.event_id }.toHashSet()
        val new = rows.filter { exist.contains(it.event_id) }
        withContext(Dispatchers.JavaFx) {
            eventAll.addAll(new)
        }
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
    suspend fun appendTimeline(timeline: Timeline<RoomEvent>) {
        val rows = timeline.events.toEventRowList(roomId)
        rows.firstOrNull()?.preceding_batch = timeline.prev_batch
        rows.firstOrNull()?.preceding_stored = timeline.limited == false

        insert(rows)

        val latest = prevLatestRow
        if (latest != null) {
            latest.following_stored = true
            data.upsert(latest)
        }
        prevLatestRow = rows.lastOrNull()
    }
}
