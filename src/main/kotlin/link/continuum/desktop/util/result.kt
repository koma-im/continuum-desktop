package link.continuum.desktop.util

import com.github.kittinunf.result.Result

typealias KResult<V, E> = Result<V, E>

class ErrorMsg(val msg: String): Exception() {
    override fun toString(): String = "Error $msg"
}

fun<T: Any> fmtErr(fmt: ()-> String): KResult<T, ErrorMsg> {
    return KResult.error(ErrorMsg(fmt()))
}

fun<T: Any, E: Exception> Ok(value: T): KResult<T, E> {
    return Result.Success<T, E>(value)
}

fun<T: Any, E: Exception> KResult<T, E>.isOk(): Boolean {
    return when (this) {
        is Result.Success -> true
        else -> false
    }
}
