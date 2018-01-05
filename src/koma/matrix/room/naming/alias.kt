package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

data class RoomAlias(val full: String) {
    val alias: String by lazy {
        full.substringBefore(':')
    }

    val servername: String by lazy {
        full.substringAfter(':', "<unknown server>")
    }
}

class RoomAliasAdapter {
    @ToJson
    fun toJson(roomId: RoomAlias): String {
        return roomId.full
    }

    @FromJson
    fun fromJson(str: String): RoomAlias {
        return RoomAlias(str)
    }
}
