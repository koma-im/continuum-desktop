package koma.util.common

import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonAdapter

fun <T: Any> JsonAdapter<T>.tryParse(json: String): Result<T, Exception> {
    try {
        val v = this.fromJson(json)
        if (v != null) {
            return Result.of(v)
        } else {
            return Result.error(NullPointerException("json parsed into null"))
        }
    } catch (e: Exception) {
        return Result.error(e)
    }
}
