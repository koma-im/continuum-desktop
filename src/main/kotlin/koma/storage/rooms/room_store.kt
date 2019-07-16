package koma.storage.rooms

import javafx.collections.FXCollections
import koma.Server
import koma.matrix.room.naming.RoomId
import link.continuum.database.KDataStore
import link.continuum.desktop.database.models.loadRoom
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.http.MediaServer
import link.continuum.libutil.`?or`
import model.Room
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class RoomStore(private val data: KDataStore){
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId, account: Account): Room {
        val newRoom = store.computeIfAbsent(roomId) {
            loadRoom(data, roomId, account) `?or` {
                logger.info { "Room $roomId not in database" }
                Room(roomId, data, account)
            }}
        return newRoom
    }
}

