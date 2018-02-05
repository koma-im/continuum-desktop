package koma.gui.view.window.chatroom.messaging.reading

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import javafx.stage.Window
import koma.gui.view.window.chatroom.messaging.reading.display.MessageCell
import koma.matrix.event.room_message.RoomEvent
import koma.storage.config.settings.AppSettings
import model.Room
import org.fxmisc.flowless.VirtualFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.reactfx.value.Val
import tornadofx.*
import kotlin.math.roundToInt

typealias MessageFlow = VirtualFlow<RoomEvent, MessageCell>

class MessagesListScrollPane(room: Room): View() {
    override val root = AnchorPane()

    private val virtualList: MessageFlow

    private val msgList: ObservableList<RoomEvent>

    private val visibleMessages: ObservableList<MessageCell>

    // decide whether to scroll
    private val followingLatest = SimpleBooleanProperty(true)
    // whether room is currently on screen
    private val showing = Val.flatMap(this.root.sceneProperty(), Scene::windowProperty).flatMap(Window::showingProperty);
    // timestamp of last message
    private var lastTime: Long = 0

    fun scrollPage(scrollDown: Boolean) {
        val dist =virtualList.height * 0.8
        if (scrollDown) {
            virtualList.scrollYBy(dist)
            onScrollDownwards()
        } else {
            virtualList.scrollYBy(-dist)
            onScrollUpwards()
        }
    }

    fun scrollToBottom() {
        followingLatest.set(true)
        virtualList.showAsLast(msgList.size)
        onScrollDownwards()
    }

    init {
        root.vgrow = Priority.ALWAYS

        msgList = room.messageManager.messages
        virtualList = VirtualFlow.createVertical(
                msgList, { MessageCell(it) }, VirtualFlow.Gravity.REAR
        )
        virtualList.vgrow = Priority.ALWAYS
        virtualList.hgrow = Priority.ALWAYS
        visibleMessages = virtualList.visibleCells()

        addVirtualScrollPane()
        addScrollBottomButton()
        setUpListeners()
        scrollToBottom()
    }

    private fun setUpListeners() {
        msgList.lastOrNull()?.let { lastTime = it.origin_server_ts }
        msgList.addListener { e: ListChangeListener.Change<out RoomEvent> -> onAddedMessages(e) }
        virtualList.setOnScroll { se ->
            if (se.deltaY > 0) {
                onScrollUpwards()
            } else if (se.deltaY < 0) {
                onScrollDownwards()
            }
        }
    }

    private fun addVirtualScrollPane() {
        val virtualScroll = VirtualizedScrollPane<MessageFlow>(virtualList)
        virtualScroll.vgrow = Priority.ALWAYS

        AnchorPane.setTopAnchor(virtualScroll, 0.0)
        AnchorPane.setLeftAnchor(virtualScroll, 0.0)
        AnchorPane.setRightAnchor(virtualScroll, 0.0)
        AnchorPane.setBottomAnchor(virtualScroll, 0.0)
        add(virtualScroll)
    }

    private fun addScrollBottomButton() {
        val scale = AppSettings.settings.scaling
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

    private fun onAddedMessages(e : ListChangeListener.Change<out RoomEvent>) {
        if (followingLatest.get()) {
            if (e.next() && e.wasAdded()) {
                val added = e.addedSubList
                val lastAdd = added.lastOrNull()?.origin_server_ts
                if (lastAdd != null && lastAdd > lastTime - 5000) {
                    //roughly latest
                    lastTime = lastAdd
                    if (added.size > 9) {
                        //added too much
                        followingLatest.set(false)
                    } else if (!showing.getOrElse(false)) {
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
     *
     * visible cells may change
     * because of mouse wheel scroll
     * or PageUp PageDown
     * or in the future swipe
     */
    private fun onScrollDownwards() {
        if (!followingLatest.get()) {
            val visibleLatest = visibleMessages.lastOrNull()?.message?.origin_server_ts
            if (visibleLatest != null && visibleLatest >= lastTime) {
                followingLatest.set(true)
            }
        }
    }

    private fun onScrollUpwards() {
        if (followingLatest.get()) {
            val visibleLatest = visibleMessages.lastOrNull()?.message?.origin_server_ts
            if (visibleLatest != null && visibleLatest < lastTime) {
                followingLatest.set(false)
            }
        }
    }
}
