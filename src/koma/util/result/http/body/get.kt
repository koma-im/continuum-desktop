package koma.util.result.http.body

import okhttp3.Response


fun Response.bodyResult(): BodyResult {
    val body = this.body()
    return if (this.isSuccessful && body != null) {
        BodyResult.Ok(body)
    } else {
        BodyResult.Error(HttpError(this))
    }
}

