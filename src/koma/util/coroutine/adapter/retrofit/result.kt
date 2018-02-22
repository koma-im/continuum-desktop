package koma.util.coroutine.adapter.retrofit

import com.github.kittinunf.result.Result
import retrofit2.Call
import ru.gildor.coroutines.retrofit.awaitResult
import ru.gildor.coroutines.retrofit.Result as RetroResult

/**
 * convert gildor's result to kittinunf's
 */
suspend fun <T : Any> Call<T>.getResult(): Result<T, Exception> {
    val res = this.awaitResult()
    return when (res) {
        is RetroResult.Ok -> Result.of(res.value)
        is RetroResult.Error -> { Result.error(res.exception) }
        is RetroResult.Exception -> { Result.of {throw res.exception} }
    }
}
