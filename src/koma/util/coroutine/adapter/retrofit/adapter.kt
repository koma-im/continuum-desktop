package koma.util.coroutine.adapter.retrofit

import com.github.kittinunf.result.Result
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

/**
 * Suspend extension for [Call] that returns a result
 */
suspend fun <T : Any> Call<T>.await(): Result<Response<T>, Exception> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>?, response: Response<T>) {
                continuation.resume(Result.of(response))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(Result.of{throw t})
            }
        })

        registerOnCompletion(continuation)
    }
}

private fun Call<*>.registerOnCompletion(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCancellation {
        if (continuation.isCancelled)
            try {
                cancel()
            } catch (ex: Throwable) {
                //Ignore cancel exception
            }
    }
}
