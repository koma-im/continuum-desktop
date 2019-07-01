package koma.util.common

import com.squareup.moshi.JsonAdapter
import koma.util.KResult

fun <T: Any> JsonAdapter<T>.tryParse(json: String): KResult<T, Exception> {
    try {
        val v = this.fromJson(json)
        if (v != null) {
            return KResult.success(v)
        } else {
            return KResult.failure(NullPointerException("json parsed into null"))
        }
    } catch (e: Exception) {
        return KResult.failure(e)
    }
}
