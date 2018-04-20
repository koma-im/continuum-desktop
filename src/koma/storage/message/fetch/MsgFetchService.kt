package koma.storage.message.fetch

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import domain.Chunked
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.naming.RoomId
import koma.storage.message.piece.Segment
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState

suspend fun doFetch(piece: Segment, roomid: RoomId)
        : Result<FetchedBatch, Exception> {
    val service = appState.apiClient
    if (service == null) {
        return Result.error(NullPointerException("no service for loading messages"))
    }
    val fetchkey = piece.prev_batch
    return if (fetchkey == null) {
        val eventid = piece.list.first().event_id
        service.getEventContext(roomid, eventid).awaitMatrix().map { res ->
            FetchedBatch.fromContextBackward(res)
        }
    } else {
        service.getRoomMessages(roomid, fetchkey, FetchDirection.Backward).awaitMatrix().map { res ->
            FetchedBatch.fromChunkedBackward(res)
        }
    }
}

class FetchedBatch(
        val messages: List<RoomEvent>,
        val prevKey: String?
) {
    companion object {
        fun fromChunkedBackward(chunked: Chunked<RoomEvent>): FetchedBatch {
            return FetchedBatch(
                    chunked.chunk.reversed(),
                    chunked.end
            )
        }

        fun fromContextBackward(res: ContextResponse): FetchedBatch {
            return FetchedBatch(
                    res.events_before.reversed(),
                    res.start
            )
        }
    }

    override fun toString(): String {
        return "FetchedBatch(messages=$messages, prevKey=$prevKey)"
    }
}
