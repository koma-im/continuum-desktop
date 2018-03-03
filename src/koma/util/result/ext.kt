package koma.util.result

import com.github.kittinunf.result.Result

fun<T: Any,E: Exception> Result<T,E>.ok(): T? =
        if (this is Result.Success)
            this.value
        else
            null
