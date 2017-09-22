package koma.storage.rooms

import javafx.collections.FXCollections
import model.Room
import java.util.concurrent.ConcurrentHashMap

object UserRoomStore {
    val roomList = FXCollections.observableArrayList<Room>()

    @Synchronized
    fun add(roomId: String): Room {
        val room = RoomStore.getOrCreate(roomId)
        if (!roomList.contains(room))
            roomList.add(room)
        return room
    }

    @Synchronized
    fun remove(roomId: String) {
        for ((index, value) in roomList.withIndex()) {
            if (value.id == roomId) {
                roomList.removeAt(index)
                break;
            }
        }
    }
}

object RoomStore{
    private val store = ConcurrentHashMap<String, Room>()

    fun getOrCreate(roomId: String): Room {
        val newRoom = store.computeIfAbsent(roomId, {Room(it)})
        return newRoom
    }

}

