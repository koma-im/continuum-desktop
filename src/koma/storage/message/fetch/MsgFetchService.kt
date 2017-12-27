package koma.storage.message.fetch

import koma.matrix.event.parse
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManager
import koma.storage.message.piece.DiscussionPiece
import koma.storage.message.piece.first_event_id
import koma_app.appState
import kotlinx.coroutines.experimental.delay
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


suspend fun MessageManager.fetchEarlier(entry: DiscussionPiece) {
    var cur = entry
    val historyNeeded = 200
    var historyFetched = 0


    loop@  while (historyFetched < historyNeeded) {
        val fetchkey = cur.prev_batch
        val res = koma.storage.message.fetch.doFetch(cur, roomid)
        if (res == null) {
            delay(1000)
            continue@loop
        }
        val (messages, next_key) = res

        if (messages.size == 0) {
            println("assume messages loading is done because response is empty")
            break@loop
        } else if (fetchkey != null && fetchkey == next_key) {
            println("finished loading room messages")
            break@loop
        }
        cur.prev_batch = next_key
        historyFetched += messages.size
        cur = this.stitcher.extendHead(cur.getKey(), messages)

        delay(1000)
    }
}

private suspend fun doFetch(piece: DiscussionPiece, roomid: RoomId): Pair<List<RoomMessage>, String>? {
    val service = appState.apiClient
    if (service == null) {
        println("no service for loading messages")
        return null
    }
    val fetchkey = piece.prev_batch
    return if (fetchkey == null) {
        val eventid = piece.first_event_id()
        eventid?: return null
        val call_res = service.getEventContext(roomid, eventid).awaitResult()
        if (call_res is Result.Ok) {
            val res = call_res.value
            val msgs = res.events_before.map { it.toMessage().parse() }.reversed()
            Pair(msgs, res.start)
        } else null
    } else {
        val call_res = service.getRoomMessages(roomid, fetchkey, FetchDirection.Backward).awaitResult()
        if (call_res is Result.Ok) {
            val res = call_res.value
            val msgs = res.chunk.map { it.toMessage().parse() }.reversed()
            Pair(msgs, res.end)
        } else null
    }
}
