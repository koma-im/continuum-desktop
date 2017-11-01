package model.room.user

/**
 * User states that are specific room
 * for example, whether a user is typing a message in a room
 */
class RoomUserState{
    var power: Double? = null
}

class RoomUserMap {
    val users = mutableMapOf<String, RoomUserState>()

    fun get(userId: String):RoomUserState {
        users.computeIfAbsent(userId, {RoomUserState()} )
        return users[userId]!!
    }
}
