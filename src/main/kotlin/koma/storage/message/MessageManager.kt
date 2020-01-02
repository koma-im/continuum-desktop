package koma.storage.message

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.transformation.SortedList
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.RawJson
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.Timeline
import koma.matrix.room.naming.RoomId
import koma.storage.message.fetch.fetchPreceding
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.toEventRowList
import link.continuum.desktop.database.FetchStatus
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.RoomMessages
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessageManager(val roomId: RoomId,
                     private val data: KDataStore,
                     private val jobQueue: RoomMessages.JobQueue
) {
    private val scope = MainScope()
    // added to UI, needs to be modified on FX thread
    private val eventAll = DedupList<RoomEventRow, String> {it.event_id}
    val shownList = SortedList(eventAll.list) { e1, e2 ->
        e1.server_time.compareTo(e2.server_time)
    }

    private var prevLatestRow: RoomEventRow? = null

    init {
    }
    private val startedFetchJobs = hashMapOf<Pair<String, FetchDirection>, FetchTask>()
    inner class FetchTask(
            private val row: RoomEventRow
    ) {
        val loading = SimpleObjectProperty<FetchStatus>(FetchStatus.NotStarted)
        val job = scope.launch(start = CoroutineStart.LAZY) { fetch() }
        private suspend fun fetch() {
            logger.debug { "fetch" }
            loading.set(FetchStatus.Started)
            val (success, failure, result) = fetchPreceding(row)
            debugAssertUiThread()
            if (!result.testFailure(success, failure)) {
                if (success.prevKey == row.preceding_batch) {
                    row.preceding_stored = true
                    logger.info { "reached earliest point at event $row" }
                    data.updateEvents(listOf(row))
                } else {
                    val rows = success.messages.toEventRowList(roomId)
                    rows.firstOrNull()?.preceding_batch = success.prevKey
                    insert(rows, tail = row)
                }
                debugAssertUiThread()
                logger.debug { "loading of messages before ${row.event_id} finished" }
                loading.set(FetchStatus.Done)
            } else {
                loading.set(FetchStatus.Failed(failure))
                logger.error { "fetch preceding events for $row $failure" }
            }
        }
    }

    /**
     * from server
     */
    @ExperimentalCoroutinesApi
    suspend fun fetchPrecedingRows(row: RoomEventRow): SimpleObjectProperty<FetchStatus> {
        debugAssertUiThread()
        val dir = FetchDirection.Backward
        val k = row.event_id to dir
        val task = startedFetchJobs.computeIfAbsent(k) { FetchTask(row) }
        jobQueue.submit(task.job)
        return task.loading
    }

    /**
     * load from db  and add messages to ui
     */
    suspend fun loadMoreFromDatabase(row: RoomEventRow?) {
        debugAssertUiThread()
        logger.debug { "loading more before $row in $roomId" }
        val lim = 100
        val latest = data.loadEvents(roomId =  roomId.full, upto =row?.server_time, limit = lim)
        logger.debug { "loaded latest ${latest.size} messages in $roomId" }
        addToUi(latest)
    }
    /**
     * add events that are new
     */
    private suspend fun addToUi(rows: List<RoomEventRow>) {
        val old = eventAll.size()
        withContext(Dispatchers.JavaFx) {
            eventAll.addAll(rows)
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
        if (rows.isEmpty()) {
            return
        }
        if (head != null) {
            rows.firstOrNull()?.preceding_stored = true
            head.following_stored = true
            data.updateEvents(listOf(head))
        }
        if (tail != null) {
            rows.lastOrNull()?.following_stored = true
            tail.preceding_stored = true
            data.updateEvents(listOf(tail))
        }
        data.updateEvents(rows)
        addToUi(rows)
    }

    /**
     * append messages returned by the sync api
     */
    suspend fun appendTimeline(timeline: Timeline<RawJson<RoomEvent>>) {
        val events = timeline.events
        if (events.isEmpty()) {
            return
        }
        val rows = events.toEventRowList(roomId)
        rows.firstOrNull()?.preceding_batch = timeline.prev_batch
        rows.firstOrNull()?.preceding_stored = timeline.limited == false
        insert(rows, head = prevLatestRow)
        logger.debug { "timeline with ${rows.size} messages, limited=${timeline.limited}" }
        prevLatestRow = rows.last()
    }
}
