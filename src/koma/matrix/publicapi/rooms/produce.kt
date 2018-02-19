package koma.matrix.publicapi.rooms

import domain.DiscoveredRoom
import koma_app.appState
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


fun getPublicRooms() = produce<DiscoveredRoom>(capacity = 1) {
    val service = appState.apiClient?.service
    service?: return@produce
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.publicRooms(since).awaitResult()
        when (call_res) {
            is Result.Ok -> {
                val roomBatch = call_res.value
                val rooms = roomBatch.chunk
                fetched += rooms.size
                println("Fetched ${rooms.size} rooms ($fetched/${roomBatch.total_room_count_estimate})")
                rooms.forEach { send(it) }
                val next = roomBatch.next_batch
                if (next == null || next == since) {
                    println("Finished fetching public rooms $fetched in total")
                    close()
                    return@produce
                }
                since = next
            }
            else -> {
                println("Error fetching public rooms")
                delay(1000)
            }
        }
    }
}
