package model

import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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
import link.continuum.database.KDataStore
import link.continuum.database.models.*
import okhttp3.HttpUrl
import tornadofx.*

class Room(
        val id: RoomId,
        private val data: KDataStore,
        aliases: List<RoomAliasRecord> = listOf(),
        name: String? = null,
        avatar: HttpUrl? = null,
        historyVisibility: HistoryVisibility? = null,
        joinRule: RoomJoinRules? = null,
        visibility: RoomVisibility? = null,
        var powerLevels: RoomPowerSettings = defaultRoomPowerSettings(id)
) {
    val canonicalAlias = SimpleObjectProperty<RoomAlias>()
    val aliases = SimpleListProperty<RoomAlias>(FXCollections.observableArrayList())
    val color = hashStringColorDark(id.toString())

    @ObsoleteCoroutinesApi
    val messageManager by lazy { MessageManager(id, appState.store.database ) }
    val members: ObservableList<UserState> = FXCollections.observableArrayList<UserState>()

    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared


    val name = SimpleStringProperty()

    // fallback in order: name, first alias, id
    private val _displayName = ReadOnlyStringWrapper(id.id)
    val displayName = _displayName.readOnlyProperty

    val avatar = SimpleObjectProperty<HttpUrl>(null)

    val users_typing = SimpleListProperty<String>(FXCollections.observableArrayList())

    init {
        this.avatar.set(avatar)
        historyVisibility?.let { histVisibility = it }
        joinRule?.let { this.joinRule = it }
        visibility?.let { this.visibility = visibility }
        this.aliases.addAll(aliases.map { RoomAlias(it.alias) })
        aliases.find { it.canonical }?.let { this.canonicalAlias.set(RoomAlias(it.alias)) }
        name?.let { this.name.set(it) }

        val alias0 = stringBinding(this.aliases) { value.getOrNull(0)?.toString() }
        val alias_id = Bindings.`when`(alias0.isNotEmpty).then(alias0).otherwise(id.toString())
        val canonstr = stringBinding(canonicalAlias) { value?.str }
        val canonAlias = Bindings.`when`(canonstr.isNotEmpty)
                .then(canonstr)
                .otherwise(alias_id)
        val n = Bindings.`when`(this.name.isNotEmpty)
                .then(name)
                .otherwise(canonAlias)
        _displayName.bind(n)
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
        val us = appState.store.userStore.getOrCreateUserId(mid)
        if (members.remove(us)) {
        } else {
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
        powerLevels.usersDefault = roomPowerLevel.users_default
        powerLevels.stateDefault = roomPowerLevel.state_default
        powerLevels.eventsDefault = roomPowerLevel.events_default
        powerLevels.ban = roomPowerLevel.ban
        powerLevels.invite = roomPowerLevel.invite
        powerLevels.kick = roomPowerLevel.kick
        powerLevels.redact = roomPowerLevel.redact
        savePowerSettings(data, powerLevels)
        saveEventPowerLevels(data, id, roomPowerLevel.events)
        saveUserPowerLevels(data, id, roomPowerLevel.users)
    }

    override fun toString(): String {
        return this.displayName.get()
    }
}


