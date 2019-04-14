package koma.storage.rooms

import javafx.collections.FXCollections
import koma.matrix.room.naming.RoomId
import link.continuum.database.KDataStore
import link.continuum.desktop.database.models.loadRoom
import link.continuum.libutil.`?or`
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

class RoomStore(private val data: KDataStore){
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId): Room {
        val newRoom = store.computeIfAbsent(roomId) {
            loadRoom(data, roomId) `?or` {
                logger.info { "Room $roomId not in database" }
                Room(roomId, data)
            }}
        return newRoom
    }
}

