package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.beans.property.SimpleObjectProperty
import koma.Koma
import koma.Server
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.storage.persistence.settings.AppSettings
import koma.storage.rooms.RoomStore
import koma.storage.users.UserStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.openStore
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.http.MediaServer
import model.Room
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

object appState {
    val currRoom = SimpleObjectProperty<Room>()
    var currentUser: UserId? = null
    lateinit var koma: Koma
    lateinit var store: AppStore
    var stopSync: (()-> Unit)? = null
    var apiClient: MatrixApi? = null

    init {
    }
}

/**
 * some components need to access the network to download avatars, etc
 */
class AppStore(
        val database: KotlinEntityDataStore<Persistable>,
        val settings: AppSettings,
        koma: Koma
        ) {
    val userStore = UserStore(database)
    /**
     * users on the network
     */
    val userData = UserDataStore(database, koma)
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
