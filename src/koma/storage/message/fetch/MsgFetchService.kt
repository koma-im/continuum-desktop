package koma.storage.message.fetch

import koma.matrix.event.parse
import koma.matrix.pagination.FetchDirection
import koma.storage.message.MessageManager
import koma.storage.message.piece.DiscussionPiece
import koma_app.appState
import kotlinx.coroutines.experimental.delay
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


suspend fun MessageManager.fetchEarlier(entry: DiscussionPiece) {
    var cur = entry
    val historyNeeded = 200
    var historyFetched = 0
    cur.prev_batch ?: return

    val service = appState.apiClient
    service?:let {
        println("no service for loading messages")
        return
    }

    loop@  while (historyFetched < historyNeeded) {
        val fetchkey = cur.prev_batch
        fetchkey?: break
        val call_res = service.getRoomMessages(roomid, fetchkey, FetchDirection.Backward).awaitResult()
        when (call_res) {
            is Result.Ok -> {
                val res = call_res.value
                val next_key = res.end
                if (res.chunk.size == 0) {
                    println("assume messages loading is done because response is empty")
                    break@loop
                } else if (fetchkey == next_key) {
                    println("finished loading room messages")
                    break@loop
                }
                cur.prev_batch = next_key
                val msgs = res.chunk.map { it.toMessage().parse() }.reversed()
                historyFetched += msgs.size
                cur = this.stitcher.extendHead(cur.getKey(), msgs)
            }
            else -> {
                delay(1000)
                continue@loop
            }
        }
        delay(1000)
    }
}
