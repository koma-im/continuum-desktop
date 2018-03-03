package koma.util.coroutine.adapter.retrofit

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.squareup.moshi.Moshi
import okio.BufferedSource
import retrofit2.Call
import retrofit2.Response

suspend fun <T : Any> Call<T>.awaitMatrix(): Result<T, Exception>
        = this.await().flatMap { it.extractBody() }


private fun<T: Any> Response<T>.extractBody(): Result<T, Exception> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.error(NullPointerException("Response body is null"))
        else Result.of(body)
    } else {
        Result.error(MatrixException(this))
    }
}

class MatrixException(code: Int, message: String?, val mxErr: MatrixError?)
    : HttpException(
        code,
        mxErrMsg(code, message, mxErr))
{
    constructor(res: Response<*>): this(
            res.code(),
            res.message(),
            MatrixError.fromSource(res.errorBody()?.source())
    )

    companion object {
        private fun mxErrMsg(code: Int, msg: String?, mxErr: MatrixError?): String =
                """
                |HTTP $code $msg
                |Matrix Error ${mxErr?.errcode} ${mxErr?.error}
                """.trimMargin()
    }
}

class MatrixError(
        val errcode: String,
        val error: String
) {
    companion object {
        private val moshi = Moshi.Builder().build()
        private val jsonAdapter = moshi.adapter(MatrixError::class.java)

        fun fromSource(bs: BufferedSource?): MatrixError? = bs?.let { jsonAdapter.fromJson(it) }
    }
}
