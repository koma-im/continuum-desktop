package koma.util.result.http.body

import koma.util.result.ReadableError
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Sealed class of HTTP result
 */
@Suppress("unused")
sealed class BodyResult {

    /**
     * Successful result of request without errors
     */
    class Ok(
            val body: ResponseBody
    ) : BodyResult() {
        override fun toString() = "BodyResult.Ok{body=$body}"
    }

    /**
     * Network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response
     */
    class Error(
            val error: HttpError
    ) : BodyResult(), ReadableError {
        override fun toString() = "BodyResult.Error{${error.message}}"
        override fun description(): String = toString()
    }
}


class HttpError(val response: Response)  {

    val code: Int
    val message: String

    init {
        code = response.code()
        message = "HTTP " + response.code() + " " + response.message()
    }
}
