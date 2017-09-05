package model

import com.smith.faktor.UserState
import domain.Chunked
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
import javafx.scene.paint.Color
import koma.graphic.getImageForName
import koma.graphic.getResizedImage
import koma.graphic.hashStringColorDark
import koma.matrix.room.naming.RoomAlias
import koma_app.appState
import koma_app.removeFirstMatching
import rx.Observable
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers
import tornadofx.ItemViewModel
import tornadofx.move

data class RoomInitialSyncResult(
        val membership: String,
        val visibility: String,
        val room_id: String,
        // when it's an invitation, the following too are nulls
        val state: List<StateMessage>,
        val messages: Chunked<Message>
)

class RoomItemModel(property: ObjectProperty<Room>) : ItemViewModel<Room>(itemProperty = property) {
    val name = bind {item?.displayName}
    val icon = bind {item?.iconProperty}
    val color = bind {item?.colorProperty}
}

class Room(val id: String) {
    // aliases have a specific format
    // there can be a canonical alias, but it's not the same as a name
    val aliases = SimpleListProperty<RoomAlias>(FXCollections.observableArrayList())
    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)

    val chatMessages: ObservableList<MessageItem> = FXCollections.observableArrayList<MessageItem>()
    val members: ObservableList<UserState> = FXCollections.observableArrayList<UserState>()

    var visibility: RoomVisibility = RoomVisibility.Private
    val joinRule: RoomJoinRules = RoomJoinRules.Invite

    val hasname = false
    val displayName = SimpleStringProperty(id)

    var hasIcon = false
    val iconURL = SimpleStringProperty("");
    val iconProperty = SimpleObjectProperty<Image>(getImageForName(id, color))

    private fun aliasesChangeActions(aliases: Observable<ObservableList<RoomAlias>>) {
        aliases.filter { it.size > 0 && !hasname }
                .map { it.get(0) }
                .subscribe {
                    displayName.set(it.full)
                    if (!hasIcon)
                        iconProperty.set(getImageForName(it.alias, color))
                }
    }

    init {
        aliasesChangeActions(aliases.toObservable())
        displayName.toObservable().filter { it.isNotBlank() && !hasIcon }
                .subscribe {
                    iconProperty.set(getImageForName(it, Color.GREEN))
                }
        iconURL.toObservable().filter { it.isNotBlank() }.observeOn(Schedulers.io())
                .map {
                    println("Room $this has new icon url $it")
                    getResizedImage(it, 32.0, 32.0)
                }
                .filterNotNull()
                .observeOnFx()
                .subscribe {
                    hasIcon = true
                    iconProperty.set(it)
                }
    }

    fun updateStates(messages: List<StateMessage>) {
        messages.forEach({applyState(it)})

    }

    private fun handleMemberStateMessage(stateMessage: StateMessage) {
        val membership = stateMessage.content["membership"] ?: ""
        when (membership) {
            "join" -> {
                val us = appState.getOrCreateUser(stateMessage.state_key)
                val displayName = stateMessage.content["displayname"] ?: stateMessage.sender.toString()
                us?.displayName?.setValue(displayName as String)
                val av = stateMessage.content["avatar_url"] as String?
                if (av != null) {
                    println("got user $displayName avatar $av through states")
                    us?.avatarURL?.set(av)
                }
                members.add(us)
            }
            "leave" -> members.removeFirstMatching { it.id.toString() == stateMessage.state_key }
            else -> println("Not handled membership message: $membership " +
                    "in ${displayName.get()} from ${stateMessage.sender} ")
        }
    }

    fun setCanonicalAlias(alias: RoomAlias) {
        if (this.aliases.contains(alias))
            this.aliases.move(alias, 0)
        else
            this.aliases.add(0, alias)
    }

    fun applyState(stateMessage: model.StateMessage) {
        when (stateMessage.type) {
            "m.room.member" -> handleMemberStateMessage(stateMessage)
            "m.room.aliases" -> {
                val maybealises = stateMessage.content["aliases"]
                if (maybealises is List<*>) {
                    val aliases = maybealises.filterIsInstance<String>()
                            .map { RoomAlias(it) }
                    this.aliases.setAll(aliases)
                }
            }
            "m.room.canonical_alias" -> {
                val alias = stateMessage.content["alias"] as String?
                if (alias != null) setCanonicalAlias(RoomAlias(alias))
            }
            "m.room.avatar" -> {
                val url = stateMessage.content["url"] as String?
                if (url != null)
                    iconURL.set(url)
            }
            else -> {
                System.err.println("Unhandled stateMessage type: " + stateMessage.type)
                System.err.println(Thread.currentThread().getStackTrace().take(2).joinToString("\n"))
            }
        }
    }
}

enum class RoomVisibility {
    Public,
    Private
}

enum class RoomJoinRules {
    Public,
    Knock,
    Invite,
    Private
}
