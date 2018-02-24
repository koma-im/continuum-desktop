package koma.matrix.room.naming

import koma.matrix.json.NewTypeString

fun canBeValidRoomId(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('!') && ss[1].isNotEmpty()
}


data class RoomId(val id: String): Comparable<RoomId>, NewTypeString(id) {
    val localstr by lazy {
        id.substringAfter('!').substringBefore(':')
    }

    val servername by lazy {
        id.substringAfter(':')
    }

    constructor(serv: String, local: String): this("!$local:$serv") {
    }

    override fun compareTo(other: RoomId): Int = this.id.compareTo(other.id)
}
