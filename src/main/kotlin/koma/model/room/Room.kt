package model

import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import koma.Server
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import koma.storage.message.MessageManager
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.KDataStore
import link.continuum.database.models.*
import link.continuum.desktop.gui.checkUiThread
import link.continuum.desktop.gui.icon.avatar.SelectUser
import link.continuum.desktop.gui.list.DedupList
import link.continuum.libutil.getOrNull
import okhttp3.HttpUrl
import tornadofx.*
import java.util.*

class Room(
        val id: RoomId,
        private val data: KDataStore,
        val account: MatrixApi,
        aliases: List<RoomAliasRecord> = listOf(),
        historyVisibility: HistoryVisibility? = null,
        joinRule: RoomJoinRules? = null,
        visibility: RoomVisibility? = null,
        var powerLevels: RoomPowerSettings = defaultRoomPowerSettings(id)
) {
    val canonicalAlias = SimpleObjectProperty<RoomAlias>()
    val aliases = DedupList<RoomAlias, RoomAlias>({it})
    val color = hashStringColorDark(id.toString())

    @ObsoleteCoroutinesApi
    val messageManager by lazy { MessageManager(this, appState.store.database ) }
    val members = DedupList<Pair<UserId, Server>, UserId>({it.first})

    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared

    /**
     * null when unknown
     * empty Optional when known to be empty
     */
    val name = SimpleObjectProperty<Optional<String>>(null)

    // fallback in order: name, first alias, id
    private val _displayName = ReadOnlyStringWrapper(id.id)
    val displayName = _displayName.readOnlyProperty

    val avatar = SimpleObjectProperty<Optional<MHUrl>>(null)

    private val server = account.server
    init {
        historyVisibility?.let { histVisibility = it }
        joinRule?.let { this.joinRule = it }
        visibility?.let { this.visibility = visibility }
        this.aliases.addAll(aliases.map { RoomAlias(it.alias) })
        aliases.find { it.canonical }?.let { this.canonicalAlias.set(RoomAlias(it.alias)) }

        val alias0 = stringBinding(this.aliases.list) {
            this.getOrNull(0)?.toString()
        }
        val alias_id = Bindings.`when`(alias0.isNotEmpty).then(alias0).otherwise(id.toString())
        val canonstr = stringBinding(canonicalAlias) { value?.str }
        val canonAlias = Bindings.`when`(canonstr.isNotEmpty)
                .then(canonstr)
                .otherwise(alias_id)
        val n0 = stringBinding(this.name) {
            value?.getOrNull()
        }
        val n = Bindings.`when`(n0.isNotNull)
                .then(n0)
                .otherwise(canonAlias)
        _displayName.bind(n)
    }


    fun makeUserJoined(us: UserId) {
        checkUiThread()
        members.add(us to server)
    }

    fun removeMember(mid: UserId) {
        checkUiThread()
        members.remove(mid to server)
    }

    fun addAlias(alias: RoomAlias) {
        checkUiThread()
        aliases.add(alias)
    }
    fun addMembers(ms: List<UserId>) {
        members.addAll(ms.map { it to server })
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

    fun setAvatar(url: String) {
        this.avatar.set(Optional.ofNullable(url.parseMxc()))
    }

    fun setAvatar(url: Optional<String>) {
        this.avatar.set(url.map { it.parseMxc() })
    }

    fun initAvatar(url: Optional<MHUrl>?) {
        this.avatar.set(url)
    }


    /**
     * when it's null, the name is considered to be unset on the server
     */
    fun setName(name: String?) {
        this.name.set(Optional.ofNullable(name))
    }

    /**
     * null means whether the room has a name is not known locally
     */
    fun initName(name: Optional<String>?) {
        this.name.set(name)
    }

    override fun toString(): String {
        return this.displayName.get()
    }
}


