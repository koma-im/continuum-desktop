package koma.util.result.http

import koma.util.result.ReadableError
import okhttp3.Response

/**
 * Sealed class of HTTP result
 */
@Suppress("unused")
sealed class HttpResult<out T : Any> {
    /**
     * Successful result of request without errors
     */
    class Ok (
            override val response: Response
    ) : HttpResult<Response>(), ResponseResult {
        override fun toString() = "HttpResult.Ok{response=$response}"
    }

    /**
     * Network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response
     */
    class Exception(
            override val exception: Throwable
    ) : HttpResult<Nothing>(), ErrorResult, ReadableError{
        override fun toString() = "HttpResult.Exception{$exception}"
        override fun description(): String = toString()
    }

}

/**
 * Interface for [HttpResult] classes with [okhttp3.Response]: [HttpResult.Ok]
 */
interface ResponseResult {
    val response: Response
}

/**
 * Interface for [HttpResult] classes that contains [Throwable]: [HttpResult.Exception]
 */
interface ErrorResult {
    val exception: Throwable
}
