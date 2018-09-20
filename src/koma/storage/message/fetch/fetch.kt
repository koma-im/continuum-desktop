package koma.storage.message.fetch

import com.github.kittinunf.result.Result
import javafx.beans.value.ObservableValue
import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManagerMsg
import koma.storage.message.PrependFetched
import koma.storage.message.piece.Segment
import koma.util.coroutine.observable.updates
import koma.util.observable.list.concat.TreeConcatList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.delay
import retrofit2.HttpException

suspend fun fetchEarlier(
        replyChan: SendChannel<MessageManagerMsg>,
        fromSegment: Segment,
        keyInView: ObservableValue<TreeConcatList.SubIndex<Long>?>,
        roomId: RoomId) {
    var segment = fromSegment
    val viewPosUpdates = keyInView.updates()
    fetch@ while (true) {
        val r = doFetch(segment, roomId)
        when (r) {
            is Result.Failure -> {
                if (r.error is HttpException) {
                    System.err.println("Can't fetch messages before $segment: ${r.error} ${r.error.message}")
                    return
                } else {
                    System.err.println("Warning fetching messages before $segment: ${r.error} ${r.error.message}")
                    delay(1000)
                    continue@fetch
                }
            }
            is Result.Success -> {
                if (r.value.prevKey == segment.meta.prev_batch) {
                    println("No more batches before $segment in $roomId")
                    break@fetch
                }
                segment.meta.prev_batch = r.value.prevKey
                val msg = PrependFetched(
                        segment.key, r.value.messages,
                        CompletableDeferred())
                replyChan.send(msg)
                val ne = msg.newEdge.await()
                ne ?: break@fetch
                segment = ne
                for (vk in viewPosUpdates.filterNotNull()) {
                    if (vk.key < segment.key) break
                    if (vk.key == segment.key && vk.subindex < 100) break
                }
            }
        }
    }
}
