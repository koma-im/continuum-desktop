package koma.util.coroutine.adapter.okhttp

import koma.util.result.http.HttpResult
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 *
 * @return HttpResult of request or throw exception
 */
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        registerOnCompletion(continuation)
    }
}

/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 *
 * @return sealed class [HttpResult] object that can be
 *         casted to [HttpResult.Ok] (success) or [HttpResult.Exception]
 */
suspend fun Call.awaitResult(): HttpResult<Response> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                continuation.resume(
                        HttpResult.Ok(response)
                )
            }

            override fun onFailure(call: Call, t: IOException) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(HttpResult.Exception(t))
            }
        })

        registerOnCompletion(continuation)
    }
}

private fun Call.registerOnCompletion(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCompletion {
        if (continuation.isCancelled)
            try {
                cancel()
            } catch (ex: Throwable) {
                //Ignore cancel exception
            }
    }
}
