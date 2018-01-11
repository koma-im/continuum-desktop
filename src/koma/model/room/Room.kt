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
import koma.matrix.event.room_message.RoomPowerLevel
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.user.identity.UserId_new
import koma.model.user.UserState
import koma.storage.message.MessageManager
import koma.storage.users.UserStore
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.room.user.RoomUserMap
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
    val room = bind { itemProperty }
}

class Room(val id: RoomId) {
    // aliases have a specific format
    // there can be a canonical alias, but it's not the same as a name
    val aliases = SimpleListProperty<RoomAlias>(FXCollections.observableArrayList())
    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)

    val messageManager = MessageManager(id)
    val members: ObservableList<UserState> = FXCollections.observableArrayList<UserState>()
    /**
     * including power levels of members
     */
    val userStates = RoomUserMap()

    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared

    val hasname = false
    val displayName = SimpleStringProperty(id.toString())

    var hasIcon = false
    val iconURL = SimpleStringProperty("");
    val iconProperty = SimpleObjectProperty<Image>(null)

    val power_levels = mutableMapOf<String, Double>()

    val users_typing = SimpleListProperty<String>(FXCollections.observableArrayList())

    private fun aliasesChangeActions(aliases: Observable<ObservableList<RoomAlias>>) {
        aliases.filter { it.size > 0 && !hasname }
                .map { it.get(0) }
                .observeOnFx()
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
        launch(JavaFx) {
            val i = getImageForName(id.toString(), color)
            iconProperty.set(i)

            displayName.toObservable().filter { it.isNotBlank() && !hasIcon }
                    .observeOnFx()
                    .subscribe {
                        iconProperty.set(getImageForName(it, Color.GREEN))
                    }
        }
        aliasesChangeActions(aliases.toObservable())

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


    /**
     * sometimes a join messages just updates an existing user and nothing needs to be done here
     */
    @Synchronized
    fun makeUserJoined(us: UserState) {
        if (!members.contains(us))
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

    fun updatePowerLevels(roomPowerLevel: RoomPowerLevel) {
        power_levels.putAll(roomPowerLevel.powerLevels.mapValues { it.value.toDouble() })
        for (user in roomPowerLevel.userLevels)
            userStates.get(UserId_new(user.key)!!).power = user.value
    }
}

enum class RoomVisibility {
    Public,
    Private
}

