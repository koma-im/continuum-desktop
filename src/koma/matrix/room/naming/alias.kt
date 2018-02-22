package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

fun canBeValidRoomAlias(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('#') && ss[1].isNotEmpty()
}

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
