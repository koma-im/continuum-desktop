package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.beans.property.SimpleObjectProperty
import koma.Koma
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import koma.storage.rooms.RoomStore
import koma.storage.rooms.UserRoomStore
import koma.storage.users.UserStore
import link.continuum.desktop.database.openStore
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

class AppStore(dir: String) {
    val database: KotlinEntityDataStore<Persistable>

    init {
        val desktop = File(dir).resolve("desktop")
        desktop.mkdirs()
        val dbPath = desktop.resolve("continuum-desktop").canonicalPath
        database = openStore(dbPath)
    }
    val userStore = UserStore(database)
    val roomStore = RoomStore(database)
    val settings = AppSettings(database)
    private val _accountRooms = mutableMapOf<UserId, UserRoomStore>()
    fun getAccountRoomStore(userId: UserId): UserRoomStore {
        return _accountRooms.computeIfAbsent(userId, { UserRoomStore(roomStore) })
    }
}
