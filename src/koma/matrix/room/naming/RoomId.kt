package koma.matrix.room.naming

data class RoomId(val id: String) {
    val localstr by lazy {
        id.substringAfter('!').substringBefore(':')
    }

    val servername by lazy {
        id.substringAfter(':')
    }

    override fun toString() = id
}

