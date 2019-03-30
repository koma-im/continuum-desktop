package model

import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.model.user.UserState
import koma.storage.message.MessageManager
import kotlinx.coroutines.ObsoleteCoroutinesApi
import tornadofx.*


class RoomItemModel(property: ObjectProperty<Room>) : ItemViewModel<Room>(itemProperty = property) {
    val name = bind {item?.displayName}
    val iconUrl = bind {item?.iconURLProperty }
    val color = bind {item?.colorProperty}
    val room = bind { itemProperty }
}

class Room(val id: RoomId) {
    val canonicalAlias = SimpleObjectProperty<RoomAlias>()
    val aliases = SimpleListProperty<RoomAlias>(FXCollections.observableArrayList())
    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)

    @ObsoleteCoroutinesApi
    val messageManager by lazy { MessageManager(id, appState.store.database ) }
    val members: ObservableList<UserState> = FXCollections.observableArrayList<UserState>()

    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared

    val name = SimpleStringProperty()

    // fallback in order: name, first alias, id
    val displayName = SimpleStringProperty(id.toString())

    val iconURLProperty = SimpleStringProperty("")
    var iconURL: String
        get() = iconURLProperty.get()
        set(value) {
            iconURLProperty.set(value)
        }

    var power_levels: RoomPowerLevelsContent? = null

    val users_typing = SimpleListProperty<String>(FXCollections.observableArrayList())

    fun sortMembers(){
        FXCollections.sort(members, { a, b -> b.weight() - a.weight() })
    }

    init {
        val alias0 = stringBinding(aliases) { value.getOrNull(0)?.toString() }
        val alias_id = Bindings.`when`(alias0.isNotEmpty).then(alias0).otherwise(id.toString())
        val canonstr = stringBinding(canonicalAlias) { value?.str }
        val canonAlias = Bindings.`when`(canonstr.isNotEmpty)
                .then(canonstr)
                .otherwise(alias_id)
        val n = Bindings.`when`(name.isNotEmpty)
                .then(name)
                .otherwise(canonAlias)
        displayName.bind(n)
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
        val us = appState.userStore.getOrCreateUserId(mid)
        if (members.remove(us)) {
        } else {
        }
    }

    fun setCanonicalAlias(alias: RoomAlias?) {
        canonicalAlias.set(alias)
        if (alias != null) {
            addAlias(alias)
        }
    }

    fun addAlias(alias: RoomAlias) {
        synchronized(aliases) {
            if (!aliases.contains(alias)) {
                aliases.add(alias)
            }
        }
    }

    fun updatePowerLevels(roomPowerLevel: RoomPowerLevelsContent) {
        this.power_levels = roomPowerLevel
    }

    override fun toString(): String {
        return this.displayName.get()
    }
}


