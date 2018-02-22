package koma.util.coroutine.adapter.retrofit

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.mapError
import com.squareup.moshi.Moshi
import retrofit2.Call
import retrofit2.HttpException

suspend fun <T : Any> Call<T>.awaitMatrix(): Result<T, Exception> {
    return this.await().mapError{ ex ->
        if (ex is HttpException) {
            MatrixException.fromHttpException(ex)
        } else {
            ex
        }
    }
}

class MatrixException(
        val httpErr: HttpException,
        val mxErr: MatrixError?
): Exception() {
    override val message: String?
        get() = ("HTTP ${httpErr.code()} ${httpErr.message()}" +
                "\nMatrix Error " +
                if (mxErr == null)
                    "null"
                else {
                    "${mxErr.errcode}: ${mxErr.error}"
                })

    companion object {
        private val moshi = Moshi.Builder()
                .build()
        private val jsonAdapter = moshi.adapter(MatrixError::class.java)

        fun fromHttpException(ex: HttpException): MatrixException {
            val me = ex.response().errorBody()?.let { jsonAdapter.fromJson(it.source()) }
            return MatrixException(ex, me)
        }
    }
}

class MatrixError(
        val errcode: String,
        val error: String
)
