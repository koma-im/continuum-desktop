package koma_app

import com.smith.faktor.UserState
import controller.ChatController
import controller.guiEvents
import domain.Chunked
import domain.ClientInitialSyncResult
import javafx.application.Platform
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import koma.matrix.UserId
import koma.matrix.room.naming.RoomAlias
import koma.matrix.user.identity.UserId_new
import koma.model.room.add_aliases_to_room
import matrix.ApiClient
import model.*
import rx.Observable
import rx.javafx.kt.addTo
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import rx.lang.kotlin.subscribeBy
import rx.schedulers.Schedulers
import tornadofx.alert
import util.getConfigDir
import java.util.*


object appState {
    val currRoom = SimpleObjectProperty<Optional<Room>>(Optional.empty())

  val currChatMessageList = SimpleObjectProperty<ObservableList<MessageItem>>()
  val currUserList = SimpleObjectProperty<ObservableList<UserState>>()

    val roomList = SimpleListProperty(FXCollections.observableArrayList<Room>())

    val userStore = HashMap<UserId, UserState>()

    val config_dir: String

    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null

    fun getOrCreateRoom(roomId: String): Room {
        val knownRoom = roomList.firstOrNull { it.id == roomId }
        if (knownRoom != null)
            return knownRoom

        val newRoom = Room(roomId)
        Platform.runLater{
            roomList.add(newRoom)
        }
        return newRoom
    }

    fun getOrCreateUserId(userId: UserId): UserState {
        val existingUser = userStore.get(userId)
        if (existingUser != null)
            return existingUser
        val newUser = UserState(userId)
        userStore.put(userId, newUser)
        return newUser
    }

    fun getOrCreateUser(useridstr: String): UserState? {
        val userid = UserId_new(useridstr)
        if (userid == null) {
            alert(Alert.AlertType.WARNING, "Invalid user id")
            return null
        } else
            return getOrCreateUserId(userid)
    }

    @Synchronized
    fun processRoomInitialSync(roomsync: RoomInitialSyncResult) {
        val room = getOrCreateRoom(roomsync.room_id)
        room.chatMessages.setAll(roomsync.messages.chunk.filterChat().map { MessageItem(it) })
        room.updateStates(roomsync.state)
        sortMembersInEachRoom()
    }

    fun sortMembersInEachRoom(){
        roomList.forEach{ room: Room ->
            FXCollections.sort(room.members, { a, b -> b.weight() - a.weight() })
        }
    }

    @Synchronized
    fun processInitialSync(initialSyncResult: ClientInitialSyncResult) {
        val roomresultss: List<RoomInitialSyncResult> = initialSyncResult.rooms
    roomresultss.forEach { room_result ->
        val room = getOrCreateRoom(room_result.room_id)
        // messages can be null
        val messages = room_result.messages
        val msg_chunk = messages.chunk
        val msg_chat = msg_chunk.filterChat()
        val msg_items = msg_chat.map { MessageItem(it) }
        room.chatMessages.setAll(msg_items)
        room.updateStates(room_result.state)
    }

        sortMembersInEachRoom()
  }

  fun processEventsResult(eventResult: Chunked<Message>, apiClient: ApiClient) {
    eventResult.chunk.forEach { message ->
      when (message.type) {
        "m.typing" -> {
          val userIds = message.content["user_ids"]
                  if (userIds != null && userIds is List<*>) {
                      val usersTyping: List<String> = userIds.map { it.toString() }
            roomList.forEach{ room: Room ->
                val users = room.members
                users.forEach { it.typing.set(usersTyping.contains(it.id.toString())) }
            }
                  }
        }
        "m.presence" -> {
            roomList.forEach{ room: Room ->
                val users = room.members
                val userId = message.content["user_id"]
                users.firstOrNull { it.id == userId }?.let {
                    it.present.set(message.content["presence"]== "online")
                    val laa = message.content["last_active_ago"]
                    it.lastActiveAgo.set(
                            if (laa is Number) {
                                laa.toLong()
                            }
                            else
                                java.lang.Long.MAX_VALUE
                    )

                }
            }
        }
        "m.room.message" -> {
              val knownRoom = roomList.firstOrNull { it.id == message.room_id }
              knownRoom?.chatMessages?.add(MessageItem(message))
        }
        "m.room.member" -> {
            val messageFromLoggedInUser = apiClient.userId == message.sender

            val users = getRoomUsers(message.room_id)

            val membership = message.content["membership"] ?: ""

            if (membership == "join") {
                // when a user updates the avatar
                // there will be a new event with membership join
                val user = getOrCreateUserId(message.sender)
                val displayName = message.content["displayname"]?.let {
                    it.toString() } ?: message.sender.toString()
                val avatarURL = message.content["avatar_url"] as String?

                user.displayName.set(displayName)
                if (avatarURL != null) {
                    println("got user $displayName avatar $avatarURL through events")
                    user.avatarURL.set(avatarURL)
                }
                if (messageFromLoggedInUser) {
                    // actually, this should only be done when the koma_app first starts
                    // or when the user joins a new room
                    Observable.just(message.room_id).addTo(guiEvents.syncRoomRequests)
                }
            } else if (membership == "leave") {
                users.removeFirstMatching { it.id == message.sender }
                if (messageFromLoggedInUser) {
                    roomList.removeFirstMatching { it.id == message.room_id }
                }
            } else if (membership == "ban") {
                val name = message.content["displayname"] ?: ""
                users.removeFirstMatching { it.displayName.get() == name }
            } else {
                println("Unhandled membership: $membership")
            }
        }
        "m.room.aliases" -> {
            val maybealiases = message.content["aliases"]
            if (maybealiases is List<*>) {
                val aliases = maybealiases.filterIsInstance<String>().map { RoomAlias(it) }
                add_aliases_to_room(message.room_id, aliases)
            }
        }
          "m.room.avatar" -> {
              val room = getOrCreateRoom(message.room_id)
              val url = message.content["url"] as String?
              if (url != null)
                room.iconURL.set(url)
          }
          "m.room.canonical_alias" -> handleCanonicalAlias(message)
        else -> {
          println("Unhandled message: " + message)

        }

      }
    }

      sortMembersInEachRoom()
  }
    private fun handleCanonicalAlias(message: Message) {
        val alias = message.content["alias"] as String?
        if (alias != null)
            getOrCreateRoom(message.room_id).setCanonicalAlias(RoomAlias(alias))
    }

  @Synchronized fun getRoomUsers(roomId: String): ObservableList<UserState> {
      val room = getOrCreateRoom(roomId)
      return room.members
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
        guiEvents.syncRoomRequests.toObservable()
                .observeOn(Schedulers.io())
                .map({ apiClient?.roomInitialSync(it) })
                .filterNotNull()
                .observeOnFx()
                .subscribe { processRoomInitialSync(it) }
  }
}

fun <T> ObservableList<T>.removeFirstMatching(predicate: (T) -> Boolean) {
  for ((index, value) in this.withIndex()) {
    if (predicate(value)) {
      this.removeAt(index)
      break;
    }
  }

}
