package koma.storage.message.fetch

import domain.Chunked
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.parse
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManager
import koma.storage.message.piece.DiscussionPiece
import koma.storage.message.piece.first_event_id
import koma_app.appState
import kotlinx.coroutines.experimental.delay
import matrix.room.RoomEvent
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await


suspend fun MessageManager.fetchEarlier(entry: DiscussionPiece) {
    var cur = entry
    val historyNeeded = 400
    var historyFetched = 0


    loop@  while (historyFetched < historyNeeded) {
        val fetchkey = cur.prev_batch
        val res = try {
            koma.storage.message.fetch.doFetch(cur, roomid)
        } catch (he: HttpException) {
            if (he.code() == 404) {
                println("stopping fetching history because of 404 at: ${entry.first_event_id()}")
                break@loop
            } else {
                delay(1000)
                continue@loop
            }
        } catch (te: Throwable) {
            te.printStackTrace()
            delay(1000)
            continue@loop
        }
        if (res == null) {
            println("stopping fetching history because of null at: ${entry.first_event_id()}")
            break@loop
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
        val res = service.getEventContext(roomid, eventid).await()
        val msgs = res.messagesInChrono()
        Pair(msgs, res.earlierKey())
    } else {
        val res = service.getRoomMessages(roomid, fetchkey, FetchDirection.Backward).await()
        val msgs = res.messagesInChrono()
        Pair(msgs, res.earlierKey())
    }
}

fun Chunked<RoomEvent>.messagesInChrono(): List<RoomMessage> {
    return this.chunk.map { it.toMessage().parse() }.reversed()
}

fun Chunked<RoomEvent>.earlierKey(): String {
    return this.end
}

fun ContextResponse.messagesInChrono(): List<RoomMessage> {
    return this.events_before.map { it.toMessage().parse() }.reversed()
}

fun ContextResponse.earlierKey(): String {
    return this.start
}


