package koma.matrix.pagination

import com.squareup.moshi.Json

enum class FetchDirection{
    @Json(name = "b") Backward,
    @Json(name = "f") Forward;

    override fun toString(): String {
        return when(this) {
            Backward -> "b"
            Forward -> "f"
        }
    }
}
