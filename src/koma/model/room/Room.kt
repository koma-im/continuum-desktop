package model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.matrix.user.identity.UserId_new
import koma.model.user.UserState
import koma.storage.message.MessageManager
import koma.storage.users.UserStore
import model.room.user.RoomUserMap
import tornadofx.*


class RoomItemModel(property: ObjectProperty<Room>) : ItemViewModel<Room>(itemProperty = property) {
    val name = bind {item?.displayName}
    val iconUrl = bind {item?.iconURLProperty }
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

    val displayName = SimpleStringProperty(id.toString())

    val iconURLProperty = SimpleStringProperty("")
    var iconURL: String
        get() = iconURLProperty.get()
        set(value) {
            iconURLProperty.set(value)
        }

    val power_levels = mutableMapOf<String, Double>()

    val users_typing = SimpleListProperty<String>(FXCollections.observableArrayList())

    fun sortMembers(){
        FXCollections.sort(members, { a, b -> b.weight() - a.weight() })
    }

    init {
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
        } else {
            println("Failed to remove $mid from ${this}")
        }
    }

    fun setCanonicalAlias(alias: RoomAlias) {
        if (this.aliases.contains(alias))
            this.aliases.move(alias, 0)
        else
            this.aliases.add(0, alias)
    }

    fun updatePowerLevels(roomPowerLevel: RoomPowerLevelsContent) {
        power_levels.putAll(roomPowerLevel.events.mapValues { it.value.toDouble() })
        for (user in roomPowerLevel.users)
            userStates.get(UserId_new(user.key)).power = user.value
    }

    override fun toString(): String {
        return this.displayName.get()
    }
}


