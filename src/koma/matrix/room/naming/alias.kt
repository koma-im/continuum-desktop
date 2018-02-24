package koma.matrix.room.naming

import koma.matrix.json.NewTypeString

fun canBeValidRoomAlias(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('#') && ss[1].isNotEmpty()
}

data class RoomAlias(val full: String): NewTypeString(full) {
    val alias: String by lazy {
        full.substringBefore(':')
    }

    val servername: String by lazy {
        full.substringAfter(':', "<unknown server>")
    }
}
