package controller

import koma.controller.events_processing.processEventsResult
import koma.controller.sync.startSyncing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import matrix.ApiClient

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

    private val shutdownSignalChan = Channel<Unit>()

    init{

    }

    fun start() {
        val start = if (apiClient.profile.hasRooms) apiClient.next_batch else null
        val syncEventChannel = startSyncing(start, shutdownSignalChan)
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in syncEventChannel) {
                apiClient.profile.processEventsResult(s)
            }
        }
    }

    fun shutdown() {
        runBlocking {
            shutdownSignalChan.send(Unit)
            shutdownSignalChan.receive()
        }
    }
}
