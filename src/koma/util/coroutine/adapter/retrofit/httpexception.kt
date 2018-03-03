package koma.util.coroutine.adapter.retrofit

import okhttp3.Response

open class HttpException(val code: Int,
                    message: String): Exception(message) {
    companion object {
        fun fromOkhttp(response: Response): HttpException {
            val code = response.code()
            val message = "HTTP $code ${response.message()}"
            return HttpException(code, message)
        }
    }
}
