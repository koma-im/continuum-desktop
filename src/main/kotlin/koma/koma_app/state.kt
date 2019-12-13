package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import koma.matrix.MatrixApi
import koma.matrix.room.naming.RoomId
import koma.storage.persistence.settings.AppSettings
import koma.storage.rooms.RoomStore
import koma.storage.users.UserStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.Account
import link.continuum.desktop.Room
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object appState {
    lateinit var store: AppStore
    val coroutineScope = CoroutineScope(Dispatchers.Main)
    var apiClient: MatrixApi? = null

    init {
    }
}

/**
 * some components need to access the network to download avatars, etc
 */
class AppStore(
        val database: KotlinEntityDataStore<Persistable>,
        val settings: AppSettings
) {
    val userStore = UserStore(database)
    /**
     * users on the network
     */
    val userData = UserDataStore(database)
    /**
     * rooms on the network
     */
    val roomStore = RoomStore(database)
    val joinedRoom = DedupList<Room, RoomId> { r -> r.id }

    fun joinRoom(roomId: RoomId, account: Account){
        joinedRoom.addIfAbsent(roomId) {
            logger.debug { "Add user joined room; $roomId" }
            roomStore.getOrCreate(it, account)
        }
    }
}
