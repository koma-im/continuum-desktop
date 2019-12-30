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
import link.continuum.desktop.Room
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.database.RoomMemberships
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.FallbackCell
import link.continuum.desktop.gui.util.UiPool
import link.continuum.desktop.util.Account
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
        val database: KotlinEntityDataStore<Persistable>,
        val settings: AppSettings
) {
    @Deprecated("")
    val userStore = UserStore(database)
    /**
     * users on the network
     */
    val userData = UserDataStore(database)
    /**
     * any known rooms on the network
     */
    val roomStore = RoomDataStorage(database, userData)
    val roomMemberships = RoomMemberships(database)
    val joinedRoom = DedupList<Room, RoomId> { r -> r.id }

    fun joinRoom(roomId: RoomId, account: Account){
        joinedRoom.addIfAbsent(roomId) {
            logger.debug { "Add user joined room; $roomId" }
            roomStore.getOrCreate(it, account)
        }
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
    val msgNotice = UiPool { MNoticeViewNode(data.database) }
    val msgText = UiPool { MTextViewNode(data.database) }
    val msgImage = UiPool { MImageViewNode() }
}
