package koma.controller.sync

import com.github.kittinunf.result.Result
import koma.matrix.sync.SyncResponse
import java.net.SocketTimeoutException

sealed class SyncStatus {
    class Resync(): SyncStatus()
    class Shutdown(): SyncStatus()
    class TransientFailure(val delay: Long, val exception: Throwable): SyncStatus()
    class Response(val response: SyncResponse): SyncStatus()
}

fun Result<SyncResponse, Exception>.inspect(): SyncStatus {
    return when (this) {
        is Result.Success -> SyncStatus.Response(this.value)
        is Result.Failure -> {
            val ex = this.error
            when (ex) {
                is SocketTimeoutException -> SyncStatus.TransientFailure(0, ex)
                else -> SyncStatus.TransientFailure(1500, ex)
            }
        }
    }
}
