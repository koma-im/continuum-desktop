package koma_app

import controller.ChatController
import javafx.beans.property.SimpleObjectProperty
import koma.Koma
import koma.koma_app.appData
import koma.matrix.UserId
import koma.storage.config.server.ServerConf
import koma.storage.rooms.RoomStore
import koma.storage.rooms.UserRoomStore
import koma.storage.users.UserStore
import matrix.ApiClient
import model.Room


object appState {
    val currRoom = SimpleObjectProperty<Room>()
    lateinit var koma: Koma
    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null
    lateinit var serverConf: ServerConf
    val roomStore by lazy { RoomStore(koma.paths) }
    private val _accountRooms = mutableMapOf<UserId, UserRoomStore>()
    val userStore by lazy { UserStore(koma.paths) }
    fun sortMembersInEachRoom(){
        val rs = accountRooms()
        rs?.forEach {  room: Room -> room.sortMembers() }
    }
    fun accountRoomStore(): UserRoomStore? {
        val u = apiClient?.profile?.userId
        u?:return null
        return getAccountRoomStore(u)
    }
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
