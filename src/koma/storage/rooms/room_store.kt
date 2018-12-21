package koma.storage.rooms

import javafx.collections.FXCollections
import koma.koma_app.SaveToDiskTasks
import koma.matrix.room.naming.RoomId
import koma.storage.config.ConfigPaths
import koma.storage.rooms.state.loadRoom
import koma.storage.rooms.state.saveRoom
import model.Room
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * rooms the user actively participate in
 */
class UserRoomStore(private val roomStore: RoomStore) {
    val roomList = FXCollections.observableArrayList<Room>()

    fun isNotEmpty() = roomList.isNotEmpty()

    @Synchronized
    fun add(roomId: RoomId): Room {
        val room = roomStore.getOrCreate(roomId)
        if (!roomList.contains(room)) {
            logger.debug { "Add user joined room; $roomId" }
            roomList.add(room)
        }
        return room
    }

    @Synchronized
    fun remove(roomId: RoomId) {
        roomList.removeIf { it.id == roomId }
    }
}

class RoomStore(private val paths: ConfigPaths){
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId): Room {
        val newRoom = store.computeIfAbsent(roomId, { paths.loadRoom(roomId)?: Room(roomId)})
        return newRoom
    }

    init {

        SaveToDiskTasks.addJob {
            this.store.forEach { _, u: Room -> paths.saveRoom(u) }
        }
    }
}

