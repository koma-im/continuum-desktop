package link.continuum.desktop.database

import koma.Failure
import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class RoomMessages(private val data: KDataStore) {
    private val jobQueue = JobQueue()
    private val messages = ConcurrentHashMap<RoomId, MessageManager>()

    fun get(roomId: RoomId): MessageManager {
        return messages.computeIfAbsent(roomId) {
            MessageManager(roomId, data, jobQueue)
        }
    }

    class JobQueue(concurrent: Int = 3) {
        /**
         * rate limit
         */
        private val semaphore = Semaphore(concurrent)
        private val tasks = mutableListOf<Job>()
        private val mutex = Mutex()
        private val scope = CoroutineScope(Dispatchers.Default)

        suspend fun submit(job: Job) {
            mutex.withLock {
                val j = tasks.indexOfLast {it === job }
                if (j > -1) {
                    val t = tasks.removeAt(j)
                    logger.debug { "moving task $j to ${tasks.size}" }
                    tasks.add(t)
                } else {
                    tasks.add(job)
                }
            }
            tryStartTasks()
        }
        private suspend fun tryStartTasks() {
            mutex.withLock {
                var l = tasks.lastIndex
                while (l > -1) {
                    val t: Job = tasks.removeAt(l)
                    if (!semaphore.tryAcquire()) {
                        tasks.add(t)
                        logger.debug { "No permits to start remaining ${tasks.size} jobs." +
                                "Available:${semaphore.availablePermits}" }
                        return
                    } else {
                        scope.launch {
                            try {
                                t.start()
                                t.join()
                            } finally {
                                semaphore.release()
                                tryStartTasks()
                            }
                        }
                    }
                    l = tasks.lastIndex
                }
            }
        }
    }
}

sealed class FetchStatus {
    object NotStarted: FetchStatus()
    object Started: FetchStatus()
    object Done: FetchStatus()
    class Failed(val failure: Failure): FetchStatus()
}