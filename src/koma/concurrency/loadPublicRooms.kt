package koma.concurrency

import domain.DiscoveredRoom
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.util.Duration
import koma.matrix.pagination.RoomBatch
import koma_app.appState


class LoadPublicRoomsService() : ScheduledService<RoomBatch<DiscoveredRoom>>() {

    private var since: String = ""
    private var last_batch_fetched = false

    init {
        this.restartOnFailure = true
        this.period = Duration.seconds(0.1)
    }

    override fun createTask(): Task<RoomBatch<DiscoveredRoom>?> {
        val task = LoadPublicRoomsTask()
        task.setOnSucceeded {
            if (last_batch_fetched) {
                this.cancel()
            }
        }
        return task
    }

    private inner class LoadPublicRoomsTask(): Task<RoomBatch<DiscoveredRoom>?>() {
        override fun call(): RoomBatch<DiscoveredRoom>? {
            val service = appState.apiClient?.service
            service?:let {
                failed()
                return null
            }

            val call_res = service.publicRooms(since).execute()
            if (!call_res.isSuccessful) {
                println("publicRooms() execute unsuccessful")
                failed()
                return null
            }
            val roombat =  call_res.body()
            if (roombat == null) {
                println("publicRooms() response body null")
                failed()
                return null
            }
            println("rb $roombat")
            val next = roombat.next_batch
            if (next == null || next == since) {
                println("finished loading public rooms")
                last_batch_fetched = true
            }
            else {
                since = next
            }
            return  roombat
        }
    }

}


