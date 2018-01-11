package koma_app

import controller.ChatController
import javafx.beans.property.SimpleObjectProperty
import matrix.ApiClient
import model.Room
import java.util.*


object appState {
    val currRoom = SimpleObjectProperty<Optional<Room>>(Optional.empty())

    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null

    fun sortMembersInEachRoom(){
        apiClient?.profile?.getRoomList()?.forEach{ room: Room -> room.sortMembers() }
    }

    init {
    }
}


