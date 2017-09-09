package model

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
import koma.matrix.UserId
import koma.matrix.room.naming.RoomAlias
import koma.model.user.UserState
import koma.storage.users.UserStore
import rx.Observable
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers
import tornadofx.*


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

    fun sortMembers(){
        FXCollections.sort(members, { a, b -> b.weight() - a.weight() })
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


    fun addMember(us: UserState) {
        members.add(us)
    }

    fun removeMember(mid: UserId) {
        val us = UserStore.getOrCreateUserId(mid)
        if (members.remove(us)) {
            println("Removed $mid from ${this.displayName}")
        } else {
            println("Failed to remove $mid from ${this.displayName}")
        }
    }

    fun setCanonicalAlias(alias: RoomAlias) {
        if (this.aliases.contains(alias))
            this.aliases.move(alias, 0)
        else
            this.aliases.add(0, alias)
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
