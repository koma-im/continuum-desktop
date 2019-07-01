package link.continuum.desktop.util

import koma.util.KResult


class ErrorMsg(val msg: String): Exception() {
    override fun toString(): String = "Error $msg"
}

fun<T: Any> fmtErr(fmt: ()-> String): KResult<T, ErrorMsg> {
    return KResult.error(ErrorMsg(fmt()))
}

fun<T: Any, E: Exception> Ok(value: T): KResult<T, E> {
    return KResult.success<T, E>(value)
}

fun<T: Any, E: Exception> Err(err: E): KResult<T, E> {
    return KResult.failure(err)
}
