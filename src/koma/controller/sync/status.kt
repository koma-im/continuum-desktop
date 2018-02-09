package koma.controller.sync

import koma.matrix.sync.SyncResponse
import ru.gildor.coroutines.retrofit.Result
import java.net.SocketTimeoutException

sealed class SyncStatus {
    class Resync(): SyncStatus()
    class Shutdown(): SyncStatus()
    class TransientFailure(val delay: Long, val message: String): SyncStatus()
    class Response(val response: SyncResponse): SyncStatus()
}

fun Result<SyncResponse>.inspect(): SyncStatus {
    return when (this) {
        is Result.Ok -> SyncStatus.Response(this.value)
        is Result.Error -> {
            val error = "http error ${this.exception.code()}: ${this.exception.message()}"
            SyncStatus.TransientFailure(500, error)
        }
        is Result.Exception -> {
            if (this.exception is SocketTimeoutException) {
                SyncStatus.TransientFailure(0, "socket timeout")
            } else {
                SyncStatus.TransientFailure(500, "exception ${this.exception.cause}")
            }
        }
    }
}
