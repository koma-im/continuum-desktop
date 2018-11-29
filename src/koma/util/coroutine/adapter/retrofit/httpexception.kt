package koma.util.coroutine.adapter.retrofit

import okhttp3.Response

open class HttpException(val code: Int,
                         override val message: String,
                         val body: String? = null
): Exception(message) {
    override fun toString(): String {
        return "HTTP $code $message"
    }
    fun toStringShowBody(): String {
        if (body == null) return "$this"
        val preview = body.take(250)
        return "$this: $preview"
    }
    companion object {
        fun fromOkhttp(response: Response): HttpException {
            val code = response.code()
            val message = response.message()
            return HttpException(code, message)
        }
    }
}
