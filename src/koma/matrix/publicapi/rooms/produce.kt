package koma.matrix.publicapi.rooms

import com.github.kittinunf.result.Result
import domain.DiscoveredRoom
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import retrofit2.HttpException

fun getPublicRooms() = produce<DiscoveredRoom>(capacity = 1) {
    val service = appState.apiClient?.service
    service?: return@produce
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.publicRooms(since).awaitMatrix()
        when (call_res) {
            is Result.Success -> {
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

fun findPublicRooms(term: String) = produce() {
    val service = appState.apiClient
    service?: return@produce
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.findPublicRooms(
                RoomDirectoryQuery(RoomDirectoryFilter(term), since = since)
                ).awaitMatrix()
        val (roomBatch, error) = call_res
        if (roomBatch != null){
            val rooms = roomBatch.chunk
            fetched += rooms.size
            println("Fetched ${rooms.size} rooms match $term ($fetched/${roomBatch.total_room_count_estimate})")
            rooms.forEach { send(it) }
            val next = roomBatch.next_batch
            if (next == null || next == since) {
                println("Finished fetching public rooms matching $term $fetched in total")
                close()
                return@produce
            }
            since = next
        }
        if (error != null) {
            if (error is HttpException) {
                println("Http Error ${error.code()} ${error.message()} finding public rooms with $term")
                close()
                return@produce
            }
            println("Error finding public rooms with $term: $error")
            delay(1000)
        }
    }
}
