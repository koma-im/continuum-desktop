package koma.gui.view.window.chatroom.messaging.reading

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import javafx.util.Callback
import koma.gui.element.control.KListView
import koma.koma_app.appState
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.desktop.database.models.RoomEventRow
import link.continuum.desktop.database.models.getEvent
import model.Room
import okhttp3.HttpUrl
import tornadofx.*
import kotlin.math.roundToInt

private val settings = appState.store.settings

typealias EventItem = RoomEventRow

@ObsoleteCoroutinesApi
class MessagesListScrollPane(
        room: Room,
        server: HttpUrl
): View() {
    override val root = AnchorPane()

    private val virtualList: KListView<EventItem>

    private val msgList: ObservableList<EventItem>

    // decide whether to scroll
    private val followingLatest = SimpleBooleanProperty(true)
    // whether room is currently on screen
    private val showing = this.root.sceneProperty().select { it.windowProperty() }.select { it.showingProperty() }
    // timestamp of last message
    private var lastTime: Long = 0

    fun scrollPage(scrollDown: Boolean) {
        val scrollRatio = 0.8f
        if (scrollDown) {
            virtualList.flow?.scroll(scrollRatio)
        } else {
            virtualList.flow?.scroll(-scrollRatio)
        }
    }

    fun scrollToBottom() {
        followingLatest.set(true)
        virtualList.scrollTo(msgList.size - 1)
    }

    init {
        root.vgrow = Priority.ALWAYS

        msgList = room.messageManager.shownList
        virtualList = KListView(msgList)
        virtualList.vgrow = Priority.ALWAYS
        virtualList.hgrow = Priority.ALWAYS

        virtualList.cellFactory = Callback<ListView<EventItem>, ListCell<EventItem>> {
            RoomEventCell(server)
        }

        addVirtualScrollPane()
        addScrollBottomButton()
        setUpListeners()
        scrollToBottom()
    }

    private fun setUpListeners() {
        lastTime = Math.max(lastTime, msgList.lastOrNull()?.server_time?: 0)
        msgList.addListener { e: ListChangeListener.Change<out EventItem> -> onAddedMessages(e) }
        virtualList.addEventFilter(ScrollEvent.SCROLL, { _: ScrollEvent ->
            onScroll()
        })
    }

    private fun addVirtualScrollPane() {
        val virtualScroll = virtualList
        virtualScroll.vgrow = Priority.ALWAYS
        AnchorPane.setTopAnchor(virtualScroll, 0.0)
        AnchorPane.setLeftAnchor(virtualScroll, 0.0)
        AnchorPane.setRightAnchor(virtualScroll, 0.0)
        AnchorPane.setBottomAnchor(virtualScroll, 0.0)
        add(virtualScroll)
    }

    private fun addScrollBottomButton() {
        val scale = settings.scaling
        val button = button() {
            removeWhen(followingLatest)
            graphic = MaterialIconFactory.get().createIcon(MaterialIcon.VERTICAL_ALIGN_BOTTOM, "${(scale*1.5).roundToInt()}em")
            style {
                backgroundRadius = multi(box(15.em))
                val size = scale * 25.px
                minWidth = size
                minHeight = size
                maxWidth = size
                maxHeight = size
            }
            action { scrollToBottom() }
        }
        AnchorPane.setRightAnchor(button, 20.0 * scale)
        AnchorPane.setBottomAnchor(button, 10.0 * scale)
    }

    private fun onAddedMessages(e : ListChangeListener.Change<out EventItem>) {
        if (followingLatest.get()) {
            if (e.next() && e.wasAdded()) {
                val added = e.addedSubList
                val lastAdd = added.lastOrNull()?.getEvent()?.origin_server_ts
                if (lastAdd != null && lastAdd > lastTime - 5000) {
                    //roughly latest
                    lastTime = lastAdd
                    if (added.size > 9) {
                        //added too much
                        followingLatest.set(false)
                    } else if (showing.value != true) {
                        //new message in a not selected room
                        followingLatest.set(false)
                    } else {
                        scrollToBottom()
                    }
                }
            }
        }
    }

    /**
     * remove the scrollToBottom button when the visible area goes downward
     */
    private fun onScroll() {
        val visibleLastIndex = virtualList.visibleIndexRange.value?.endInclusive
        visibleLastIndex ?:return
        val loadedLastIndex = msgList.size - 1
        if (visibleLastIndex >= loadedLastIndex - 2) {
            followingLatest.set(true)
        } else {
            followingLatest.set(false)
        }
    }
}
