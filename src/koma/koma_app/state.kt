package koma_app

import controller.ChatController
import javafx.beans.property.SimpleObjectProperty
import koma.storage.rooms.UserRoomStore
import matrix.ApiClient
import model.Room
import util.getConfigDir
import java.util.*


object appState {
    val currRoom = SimpleObjectProperty<Optional<Room>>(Optional.empty())

    val config_dir: String

    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null

    fun sortMembersInEachRoom(){
        UserRoomStore.roomList.forEach{ room: Room -> room.sortMembers() }
    }

    init {
        config_dir = getConfigDir()
    }
}


