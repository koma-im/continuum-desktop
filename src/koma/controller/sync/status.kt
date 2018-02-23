package koma.controller.sync

import koma.matrix.sync.SyncResponse
import ru.gildor.coroutines.retrofit.Result
import java.net.SocketTimeoutException

sealed class SyncStatus {
    class Resync(): SyncStatus()
    class Shutdown(): SyncStatus()
    class TransientFailure(val delay: Long, val exception: Throwable): SyncStatus()
    class Response(val response: SyncResponse): SyncStatus()
}

fun Result<SyncResponse>.inspect(): SyncStatus {
    return when (this) {
        is Result.Ok -> SyncStatus.Response(this.value)
        is Result.Error -> {
            SyncStatus.TransientFailure(500, this.exception)
        }
        is Result.Exception -> {
            val ex = this.exception
            if (ex is SocketTimeoutException) {
                SyncStatus.TransientFailure(0, ex)
            } else {
                SyncStatus.TransientFailure(500, ex)
            }
        }
    }
}
