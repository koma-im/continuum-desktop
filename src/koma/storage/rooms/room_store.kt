package koma.storage.rooms

import javafx.collections.FXCollections
import koma.koma_app.SaveJobs
import koma.matrix.room.naming.RoomId
import koma.storage.rooms.state.loadRoom
import koma.storage.rooms.state.save
import model.Room
import java.util.concurrent.ConcurrentHashMap

/**
 * rooms the user actively participate in
 */
class UserRoomStore {
    val roomList = FXCollections.observableArrayList<Room>()

    @Synchronized
    fun add(roomId: RoomId): Room {
        val room = RoomStore.getOrCreate(roomId)
        if (!roomList.contains(room))
            roomList.add(room)
        return room
    }

    @Synchronized
    fun remove(roomId: RoomId) {
        roomList.removeIf { it.id == roomId }
    }
}

object RoomStore{
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId): Room {
        val newRoom = store.computeIfAbsent(roomId, { loadRoom(roomId)?: Room(roomId)})
        return newRoom
    }

    init {

        SaveJobs.addJob {
            this.store.forEach { _, u: Room -> u.save() }
        }
    }
}

