package link.continuum.desktop.gui.message


import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Region
import javafx.scene.text.Text
import koma.Failure
import koma.gui.view.window.chatroom.messaging.reading.display.EventSourceViewer
import koma.gui.view.window.chatroom.messaging.reading.display.GuestAccessUpdateView
import koma.gui.view.window.chatroom.messaging.reading.display.HistoryVisibilityEventView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.koma_app.AppStore
import koma.matrix.event.room_message.*
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import link.continuum.database.models.RoomEventRow
import link.continuum.desktop.database.FetchStatus
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.view.AccountContext
import link.continuum.desktop.util.gui.alert
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val sourceViewer by lazy { EventSourceViewer() }

class FallbackCell():MessageCellContent<RoomEvent> {
    private val text= Text()
    override val root = HBox(5.0).apply {
        alignment = Pos.CENTER
        add(text)
    }

    override fun update(message: RoomEvent, server: MediaServer) {
        text.text= message.type.toString()
    }
}

interface MessageCellContent<T> {
    val root: Node
    fun menuItems(): List<MenuItem> = listOf()
    fun update(message: T, server: MediaServer)
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
open class MessageCell(
        private val context: AccountContext,
        protected val store: AppStore
): ListCell<RoomEventRow>() {
    private val scope = MainScope()
    protected open val center: Region = StackPane()
    private val loading = LoadingStatus()

    val node = VBox(3.0).apply {
        hbox {
            alignment = Pos.CENTER
            add(loading.root)
        }
    }
    protected val contextMenuShowSource = MenuItem("View Source").apply {
        setOnAction {
            current?.let {
                sourceViewer.showAndWait(it)
            }
        }
    }
    protected val menu: ContextMenu = ContextMenu()
    protected var current: RoomEventRow? = null
    private var currentContent: Any? = null

    override fun updateItem(item: RoomEventRow?, empty: Boolean) {
        super.updateItem(item, empty)
        releaseContentCell()
        if (empty || item == null) {
            graphic = null
            return
        }
        val event = item.getEvent() ?: return
        val server = context.account.server
        val content = when (event) {
            is MRoomMessage -> store.messageCells.take().apply {
                update(event, server)
            }
            is MRoomMember -> store.membershipCells.take().apply {
                update(event, server)
            }
            is MRoomGuestAccess -> store.guestAccessCells.take().apply {
                update(event, server)
            }
            is MRoomHistoryVisibility -> store.historyVisibilityCells.take().apply {
                update(event, server)
            }
            else -> store.fallbackCells.take().apply {
                update(event, server)
            }
        }
        currentContent = content
        node.children.run {
            if (size > 1) set(1, content.root)
            else add(content.root)
            Unit
        }
        updateEvent(item)
        graphic = node
    }
    private fun releaseContentCell() {
        val c = currentContent?: return
        currentContent = null
        when (c) {
            is MRoomMessageViewNode -> store.messageCells.pushBack(c)
            is MRoomMemberViewNode -> store.membershipCells.pushBack(c)
            is GuestAccessUpdateView -> store.guestAccessCells.pushBack(c)
            is HistoryVisibilityEventView -> store.historyVisibilityCells.pushBack(c)
            is FallbackCell -> store.fallbackCells.pushBack(c)
            else -> error("Unexpected node $c")
        }
    }
    private fun updateEvent(message: RoomEventRow) {
        loading.clear()
        if(!message.preceding_stored) {
            logger.trace { "messages before ${message.event_id} are not stored yet" }
            val messageManager = store.messages.get(RoomId(item.room_id))
            scope.launch {
                val status = messageManager.fetchPrecedingRows(message)
                loading.update(status)
            }
        }
        current = message
    }
    init {
        node.setOnContextMenuRequested { event ->
            val c = currentContent
            if (c is MessageCellContent<*>) {
                menu.items.run {
                    clear()
                    addAll(c.menuItems())
                    add(contextMenuShowSource)
                }
            }
            menu.show(node, event.screenX, event.screenY)
            event.consume()
        }
    }
}

class LoadingStatus {
    private val icon = ProgressIndicator().apply {
        style = iconStyle
    }
    private val label = Label("Waiting to loading messages...").apply {
        alignment = Pos.CENTER
    }
    private var failure: Failure? = null
    private val failButton = Hyperlink("Info")
    val root = HBox(label, icon).apply {
        spacing = 5.0
        alignment = Pos.CENTER
    }
    private val status = SimpleObjectProperty<FetchStatus>(FetchStatus.NotStarted)
    init {
        failButton.setOnAction {
            alert(Alert.AlertType.ERROR, "Failure", failure?.message)
        }
        status.addListener { _, _, newValue ->
            val show = when(newValue) {
                FetchStatus.NotStarted -> {
                    label.text = "Waiting to loading messages..."
                    root.children.setAll(label)
                }
                FetchStatus.Started -> {
                    label.text = "Loading messages..."
                    root.children.setAll(label, icon)
                }
                FetchStatus.Done -> false
                is FetchStatus.Failed -> {
                    failure = newValue.failure
                    label.text = "Failed to load messages"
                    root.children.setAll(label, failButton)
                }
            }
            root.showIf(show)
        }
    }
    fun clear(){
        root.showIf(false)
    }
    fun update(status: SimpleObjectProperty<FetchStatus>) {
        this.status.unbind()
        this.status.set(status.value)
        this.status.bind(status)
        root.showIf(true)
    }
    companion object {
        private val iconStyle = StyleBuilder {
            fixWidth(1.em)
            fixHeight(1.em)
        }.toStyle()
    }
}