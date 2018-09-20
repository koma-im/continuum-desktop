package koma.util.coroutine.adapter.okhttp

import com.github.kittinunf.result.Result
import koma.util.coroutine.adapter.retrofit.HttpException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import kotlin.coroutines.resume


/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 */
suspend fun Call.await(): Result<Response, Exception> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                continuation.resume(Result.of (response))
            }

            override fun onFailure(call: Call, t: IOException) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(Result.error(t))
            }
        })

        registerOnCompletion(continuation)
    }
}

fun Response.extract(): Result<ResponseBody, Exception> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.error(NullPointerException("Response body is null"))
        else Result.of(body)
    } else {
        Result.error(HttpException.fromOkhttp(this))
    }
}

private fun Call.registerOnCompletion(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCancellation {
        if (continuation.isCancelled)
            try {
                cancel()
            } catch (ex: Throwable) {
                //Ignore cancel exception
            }
    }
}
