package link.continuum.desktop.action

import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.gui.view.ChatView
import koma.koma_app.AppStore
import koma.matrix.MatrixApi
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import link.continuum.database.models.getSyncBatchKey
import link.continuum.database.models.saveSyncBatchKey
import mu.KotlinLogging

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
    private var job: Job
    private val mutex = Mutex()
    private var sync: MatrixSyncReceiver
    init{
        val batch_key = getSyncBatchKey(appData.database, apiClient.userId)
        sync = MatrixSyncReceiver(apiClient, batch_key)
        coroutineScope.startProcessing()
        job = coroutineScope.launch {
            sync.startSyncing()
        }
    }

    suspend fun start(full_sync: Boolean = false) {
        mutex.withLock {
            val batch_key = if (full_sync) {
                null
            } else {
                getSyncBatchKey(appData.database, apiClient.userId)
            }
            job.cancel()
            sync = MatrixSyncReceiver(apiClient, batch_key)
            job = coroutineScope.launch {
                sync.startSyncing()
            }
        }
    }

    @ObsoleteCoroutinesApi
    private fun CoroutineScope.startProcessing() {
        launch(Dispatchers.JavaFx) {
            for ((s, f, r) in sync.events) {
                if (!r.testFailure(s, f)) {
                    processEventsResult(s, apiClient, appData = appData, view = view)
                    val nb = sync.since
                    nb?.let {
                        saveSyncBatchKey(appData.database, apiClient.userId, nb)
                    }
                } else {
                    logger.warn { "sync issue $f" }
                }
            }
            logger.debug { "events are no longer processed" }
        }
    }
}
