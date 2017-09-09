package koma_app

import controller.ChatController
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import koma.model.user.UserState
import koma.storage.rooms.RoomStore
import matrix.ApiClient
import model.MessageItem
import model.Room
import rx.javafx.kt.toObservable
import rx.lang.kotlin.subscribeBy
import util.getConfigDir
import java.util.*


object appState {
    val currRoom = SimpleObjectProperty<Optional<Room>>(Optional.empty())

    val currChatMessageList = SimpleObjectProperty<ObservableList<MessageItem>>()
    val currUserList = SimpleObjectProperty<ObservableList<UserState>>()


    val config_dir: String

    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null

    fun sortMembersInEachRoom(){
        RoomStore.forEach{ room: Room -> room.sortMembers() }
    }

    init {
        config_dir = getConfigDir()
        currRoom.toObservable().subscribeBy(onNext= {
            if (it.isPresent) {
                val registeredRoom = it.get()
                currChatMessageList.set(registeredRoom.chatMessages)
                currUserList.set(registeredRoom.members)
            } else {
                currChatMessageList.set(SimpleListProperty<MessageItem>())
                currUserList.set(SimpleListProperty<UserState>())
            }
        })
  }
}


