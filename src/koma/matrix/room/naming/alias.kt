package koma.matrix.room.naming

data class RoomAlias(val full: String) {
    val alias: String by lazy {
        full.substringBefore(':')
    }

    val servername: String by lazy {
        full.substringAfter(':', "<unknown server>")
    }
}
