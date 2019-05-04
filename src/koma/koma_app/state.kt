package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.beans.property.SimpleObjectProperty
import koma.Koma
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

@ExperimentalCoroutinesApi
class AppStore(dir: String) {
    val database: KotlinEntityDataStore<Persistable>

    init {
        val desktop = File(dir).resolve("desktop")
        desktop.mkdirs()
        val dbPath = desktop.resolve("continuum-desktop").canonicalPath
        database = openStore(dbPath)
    }
    val userStore = UserStore(database)
    /**
     * users on the network
     */
    val userData = UserDataStore(database)
    /**
     * rooms on the network
     */
    val roomStore = RoomStore(database)
    val settings = AppSettings(database)
    val joinedRoom = DedupList<Room, RoomId> { r -> r.id }

    fun joinRoom(roomId: RoomId){
        joinedRoom.addIfAbsent(roomId) {
            logger.debug { "Add user joined room; $roomId" }
            roomStore.getOrCreate(it)
        }
    }
}
