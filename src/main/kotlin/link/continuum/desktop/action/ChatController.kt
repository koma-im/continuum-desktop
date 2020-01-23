package link.continuum.desktop.action

import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.gui.view.ChatView
import koma.koma_app.AppStore
import koma.matrix.MatrixApi
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import link.continuum.database.models.getSyncBatchKey
import link.continuum.database.models.saveSyncBatchKey
import mu.KotlinLogging
import kotlin.time.MonoClock
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

/**
 * loads sync pagination token from disk by user id
 * Created by developer on 2017/6/22.
 */
@ExperimentalCoroutinesApi
class SyncControl(
        val apiClient: MatrixApi,
        private val appData: AppStore,
        parentJob: Job,
        private val view: ChatView
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + parentJob)
    private val synchronizer = FlowSynchronizer(apiClient)
    init{
        startProcessing()
        coroutineScope.launch {
            val batch_key = appData.database.letOp { getSyncBatchKey(it, apiClient.userId)  }
            synchronizer.start(batch_key)
        }
    }

    fun restartInitial() = synchronizer.restartInitial()

    @ObsoleteCoroutinesApi
    private fun startProcessing() {
        synchronizer.syncFlow.onEach {
            val (s, f, r) = it
            if (!r.testFailure(s, f)) {
                processEventsResult(s, apiClient, appData = appData, view = view)
                appData.database.letOp {
                    saveSyncBatchKey(it, apiClient.userId, s.next_batch)
                }
            } else {
                logger.warn { "sync issue $f" }
            }
        }.onCompletion {
            logger.debug { "events are no longer processed" }
        }.launchIn(coroutineScope)
    }
}



/**
 * detect computer suspend and resume
 */
internal fun CoroutineScope.detectTimeLeap(): Channel<Unit> {
    val timeleapSignal = Channel<Unit>(Channel.CONFLATED)
    var prev = System.currentTimeMillis()
    launch {
        while (true) {
            delay(1000)
            val now = System.currentTimeMillis()
            val elapsed = now - prev
            if (elapsed > 20000) {
                logger.info { "System time leapt from $prev to $now" }
                timeleapSignal.send(Unit)
            }
            prev = now
        }
    }
    return timeleapSignal
}

class FlowSynchronizer(
        private val client: MatrixApi
) {
    var since: String? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val startSignal = Channel<Boolean>(Channel.CONFLATED)
    val syncFlow = startSignal.consumeAsFlow().onEach {
        logger.info { "start syncing, force initial sync is $it" }
    }.flatMapLatest {
        if (it) since = null
        startSyncing()
    }.buffer(4)
    init {
        scope.launch {
            for (i in detectTimeLeap()) {
                startSignal.send(false)
            }
        }
    }
    fun start(from: String?) {
        since = from
        startSignal.offer(false)
    }
    /*********************************
     * slow
     */
    fun restartInitial() {
        startSignal.offer(true)
    }
    private suspend fun startSyncing() = flow {
        var timeout = 10.seconds
        while (true) {
            val startTime = MonoClock.markNow()
            val (syncRes, error, result) =  client.sync(since, timeout = timeout)
            emit(result)
            if (syncRes!=null) {
                since = syncRes.next_batch
                timeout = 50.seconds
            } else {
                check(error != null)
                timeout = 1.seconds
                logger.warn { "Sync failure: $error" }
                val minInterval = 1.seconds // limit rate of retries
                val dur = startTime.elapsedNow()
                if (dur < minInterval) {
                    val remaining = minInterval - dur
                    delay(remaining.toLongMilliseconds())
                }
            }
        }
    }
}