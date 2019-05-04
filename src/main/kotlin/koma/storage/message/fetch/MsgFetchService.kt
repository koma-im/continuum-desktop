package koma.storage.message.fetch

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import koma.koma_app.appState
import koma.matrix.Chunked
import koma.matrix.event.EventId
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.pagination.FetchDirection
import koma.matrix.room.naming.RoomId
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import link.continuum.database.models.RoomEventRow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun fetchPreceding(
        row: RoomEventRow)
        : Result<FetchedBatch, Exception> {
    val service = appState.apiClient
    if (service == null) {
        return Result.error(NullPointerException("no service for loading messages"))
    }
    val fetchkey = row.preceding_batch
    return if (fetchkey == null) {
        val eventid = row.event_id
        logger.warn { "trying to get pagination token by getting the context of $eventid" }
        service.getEventContext(RoomId(row.room_id), EventId(eventid)).awaitMatrix().map { res ->
            FetchedBatch.fromContextBackward(res)
        }
    } else {
        service.getRoomMessages(RoomId(row.room_id), fetchkey, FetchDirection.Backward).awaitMatrix().map { res ->
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
