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

fun<T: Any, E: Exception> Err(err: E): KResult<T, E> {
    return Result.Failure(err)
}

fun<T: Any, E: Exception> KResult<T, E>.isOk(): Boolean {
    return when (this) {
        is Result.Success -> true
        else -> false
    }
}

inline infix fun<T: Any, E: Exception> KResult<T, E>.getOr(action: (Result.Failure<T, E>) -> T): T {
    return when(this) {
        is Result.Success -> this.value
        is Result.Failure -> action(this)
    }
}

inline infix fun<T: Any, E: Exception> KResult<T, E>.getErrOr(action: (Result.Success<T, E>) -> E): E {
    return when(this) {
        is Result.Success -> action(this)
        is Result.Failure -> return this.error
    }
}
