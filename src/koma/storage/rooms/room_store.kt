package koma.storage.rooms

import javafx.collections.FXCollections
import koma.matrix.room.naming.RoomId
import koma.storage.rooms.state.load_rooms
import koma.storage.rooms.state.save
import model.Room
import java.util.concurrent.ConcurrentHashMap

/**
 * rooms the user actively participate in
 */
object UserRoomStore {
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
        for ((index, value) in roomList.withIndex()) {
            if (value.id == roomId) {
                roomList.removeAt(index)
                break;
            }
        }
    }
}

object RoomStore{
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId): Room {
        val newRoom = store.computeIfAbsent(roomId, {Room(roomId)})
        return newRoom
    }

    init {
        for (r in load_rooms()) {
            store.put(r.id, r)
        }

        Runtime.getRuntime().addShutdownHook(Thread({
            this.store.forEach { _, u: Room -> u.save() }
        }))
    }
}

