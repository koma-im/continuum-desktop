package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import koma.gui.view.window.chatroom.messaging.reading.display.GuestAccessUpdateView
import koma.gui.view.window.chatroom.messaging.reading.display.HistoryVisibilityEventView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MEmoteViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MImageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MNoticeViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MTextViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.matrix.MatrixApi
import koma.matrix.room.naming.RoomId
import koma.storage.persistence.settings.AppSettings
import koma.storage.users.UserStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.database.RoomMemberships
import link.continuum.desktop.database.RoomMessages
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.FallbackCell
import link.continuum.desktop.gui.util.UiPool
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object appState {
    lateinit var store: AppStore
    val job = SupervisorJob()
    val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    var apiClient: MatrixApi? = null

    init {
    }
}

typealias AppStore = AppData
/**
 * data
 */
class AppData(
        db: KotlinEntityDataStore<Persistable>,
        val settings: AppSettings
) {
    val database = KDataStore(db)
    @Deprecated("")
    val userStore = UserStore()
    /**
     * users on the network
     */
    val userData = UserDataStore(database)
    /**
     * any known rooms on the network
     */
    val roomStore = RoomDataStorage(database, this, userData)
    val roomMemberships = RoomMemberships(database)
    /**
     * map of room id to message manager
     */
    val messages = RoomMessages(database)
    val joinedRoom = DedupList<RoomId, RoomId> { it }

    fun joinRoom(roomId: RoomId){
        joinedRoom.addIfAbsent(roomId) { it }
    }

    // reuse components in ListView of events
    val messageCells = UiPool{ MRoomMessageViewNode(this) }
    val membershipCells = UiPool{ MRoomMemberViewNode(this) }
    val guestAccessCells = UiPool{ GuestAccessUpdateView(this) }
    val historyVisibilityCells = UiPool{ HistoryVisibilityEventView(this)}
    val fallbackCells = UiPool{ FallbackCell() }
    val uiPools = ComponentPools(this)
}

/**
 * some components need to access data to get avatar urls, etc.
 */
class ComponentPools(private val data: AppData){
    // UI for content of messages
    val msgEmote = UiPool { MEmoteViewNode(data.userData) }
    val msgNotice = UiPool { MNoticeViewNode(data.userData) }
    val msgText = UiPool { MTextViewNode(data.userData) }
    val msgImage = UiPool { MImageViewNode() }
}
