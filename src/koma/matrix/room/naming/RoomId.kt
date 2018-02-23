package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

fun canBeValidRoomId(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('!') && ss[1].isNotEmpty()
}


data class RoomId(val id: String): Comparable<RoomId> {
    val localstr by lazy {
        id.substringAfter('!').substringBefore(':')
    }

    val servername by lazy {
        id.substringAfter(':')
    }

    constructor(serv: String, local: String): this("!$local:$serv") {
    }

    override fun toString() = id

    override fun compareTo(other: RoomId): Int = this.id.compareTo(other.id)
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
