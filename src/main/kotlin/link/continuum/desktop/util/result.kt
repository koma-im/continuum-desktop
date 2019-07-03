package link.continuum.desktop.util

import koma.Failure
import koma.OtherFailure
import koma.util.KResult

fun<T: Any> fmtErr(fmt: ()-> String): KResult<T, Failure> {
    return KResult.failure(OtherFailure(fmt()))
}

fun<T: Any, E: Any> Ok(value: T): KResult<T, E> {
    return KResult.success<T, E>(value)
}

fun<T: Any, E: Any> Err(err: E): KResult<T, E> {
    return KResult.failure(err)
}
