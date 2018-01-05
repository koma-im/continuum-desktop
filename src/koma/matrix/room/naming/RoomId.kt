package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

data class RoomId(val id: String) {
    val localstr by lazy {
        id.substringAfter('!').substringBefore(':')
    }

    val servername by lazy {
        id.substringAfter(':')
    }

    constructor(serv: String, local: String): this("!$local:$serv") {
    }

    override fun toString() = id
}

class RoomIdAdapter {
    @ToJson fun toJson(roomId: RoomId): String {
        return roomId.id
    }

    @FromJson
    fun fromJson(str: String): RoomId {
        return RoomId(str)
    }
}
