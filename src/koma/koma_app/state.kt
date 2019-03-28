package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.beans.property.SimpleObjectProperty
import koma.Koma
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.storage.config.server.ServerConf
import koma.storage.rooms.RoomStore
import koma.storage.rooms.UserRoomStore
import koma.storage.users.UserStore
import model.Room
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object appState {
    val currRoom = SimpleObjectProperty<Room>()
    var currentUser: UserId? = null
    lateinit var koma: Koma
    lateinit var data: KotlinEntityDataStore<Persistable>
    var stopSync: (()-> Unit)? = null
    var apiClient: MatrixApi? = null
    lateinit var serverConf: ServerConf
    val roomStore by lazy { RoomStore(koma.paths) }
    private val _accountRooms = mutableMapOf<UserId, UserRoomStore>()
    val userStore by lazy { UserStore(koma.paths) }
    fun sortMembersInEachRoom(){
        val rs = accountRooms()
        rs?.forEach {  room: Room -> room.sortMembers() }
    }
    fun accountRoomStore(): UserRoomStore? {
        val u = currentUser
        if (u == null) {
            logger.warn { "user id not set, can't manage joined rooms" }
            return null
        }
        return getAccountRoomStore(u)
    }

    /**
     * only creates an object
     * does not load rooms from disk
     */
    fun getAccountRoomStore(userId: UserId): UserRoomStore? {
        return _accountRooms.computeIfAbsent(userId, { UserRoomStore(roomStore) })
    }
    fun accountRooms(): List<Room>? {
        return accountRoomStore()?.roomList
    }

    init {
    }
}

val AppSettings by lazy { appData.settings }
