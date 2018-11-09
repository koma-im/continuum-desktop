package koma.matrix.user.auth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.squareup.moshi.Moshi
import koma.util.coroutine.adapter.retrofit.HttpException
import koma.util.coroutine.adapter.retrofit.MatrixError
import koma.util.coroutine.adapter.retrofit.await
import okio.BufferedSource
import retrofit2.Call


/**
 * the server may return instructions for further authentication
 */
suspend fun <T : Any> Call<T>.awaitMatrixAuth(): Result<T, Exception> {
    val result = this.await()
    return result.flatMap { response ->
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null)
                return@flatMap Result.error(NullPointerException("Response body is null"))
            return@flatMap Result.of(body)
        }

        if (response.code() == 401) {
            val unauth = Unauthorized.fromSource(response.errorBody()?.source())
            if (unauth != null)
                return@flatMap Result.error(AuthException.AuthFail(unauth))
        }
        val m = MatrixError.fromSource(response.errorBody()?.source())
        if (m!=null) return@flatMap Result.error(AuthException.MatrixFail(m))
        val h = HttpException.fromOkhttp(response.raw())
        return@flatMap Result.error(AuthException.HttpFail(h))
    }
}


data class Unauthorized(
        // Need more stages
        val completed: List<String>?,
        // Try again, for example, incorrect passwords
        val errcode: String?,
        val error: String?,
        val flows: List<AuthFlow<String>>,
        val params: Map<String, Any>,
        val session: String?
) {
    fun flows(): List<AuthFlow<AuthType>> {
        return flows.map { flow ->
            AuthFlow<AuthType>(flow.stages.map { stage -> AuthType.parse(stage) })
        }
    }
    companion object {
        private val moshi = Moshi.Builder().build()
        private val jsonAdapter = moshi.adapter(Unauthorized::class.java)

        fun fromSource(bs: BufferedSource?): Unauthorized? = bs?.let { jsonAdapter.fromJson(it) }
    }
}

data class AuthFlow<T>(
        val stages: List<T>
)

sealed class AuthType(val type: String) {
    class Dummy(t: String): AuthType(type=t)
    class Email(t: String): AuthType(t)
    class Recaptcha(t: String): AuthType(t)
    class Other(type: String): AuthType(type)

    companion object {
        fun parse(s: String): AuthType {
            return when (s) {
                "m.login.dummy" -> Dummy(s)
                "m.login.recaptcha" -> Recaptcha(s)
                "m.login.email.identity" -> Email(s)
                else -> Other(s)
            }
        }
    }

    fun toDisplay(): String {
        return when (this) {
            is Dummy -> "Password"
            is Email -> "Email"
            is Recaptcha -> "Captcha"
            is Other -> this.type
        }
    }
}

sealed class AuthException(message: String): Exception(message) {
    class AuthFail(
            val status: Unauthorized): AuthException(status.toString())
    class MatrixFail(
            val error: MatrixError): AuthException(error.toString())
    // Other http errors
    class HttpFail(
            val error: HttpException): AuthException(error.message)
}
