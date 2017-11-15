package koma.storage.message.fetch

import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.util.Duration
import koma.matrix.event.parse
import koma.matrix.event.room_message.timeline.FetchedMessages
import koma.matrix.pagination.FetchDirection
import koma_app.appState

class LoadRoomMessagesService(
        val roomId: String,
        var since: String,
        val direction: FetchDirection,
        val limit_key: String?
) : ScheduledService<FetchedMessages?>() {

    private var last_batch_fetched = false

    init {
        this.restartOnFailure = true
        this.period = Duration.seconds(0.9)
    }

    override fun createTask(): Task<FetchedMessages?> {
        val task = LoadRoomMessagesTask()
        task.setOnSucceeded {
            if (last_batch_fetched) {
                this.cancel()
            }
        }
        return task
    }

    private inner class LoadRoomMessagesTask(): Task<FetchedMessages?>() {
        override fun call(): FetchedMessages? {
            val service = appState.apiClient
            service?:let {
                println("no service for loading messages")
                failed()
                return null
            }

            val call_res = service.getRoomMessages(roomId, since, direction, limit_key)
            if (call_res == null) {
                println("failed to get messages")
                failed()
                return null
            }
            val next = call_res.end
            if (call_res.chunk.size == 0) {
                println("assume messages loading is done because response is empty")
                last_batch_fetched = true
            } else if (next == since) {
                println("finished loading room messages")
                last_batch_fetched = true
            } else {
                since = next
            }
            succeeded()
            val ret = FetchedMessages(
                    end = call_res.end,
                    finished = last_batch_fetched,
                    messages = call_res.chunk.map { it.toMessage().parse() }.reversed()
            )
            return ret
        }
    }

}


