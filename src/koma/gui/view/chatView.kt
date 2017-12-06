package koma.gui.view

import controller.guiEvents
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.controller.requests.sendMessage
import koma.gui.view.messagesview.fragment.MessageCell
import koma.gui.view.messagesview.fragment.create_message_cell
import koma.input.emoji.EmojiPanel
import koma.matrix.event.room_message.RoomMessage
import koma_app.appState
import model.Room
import org.fxmisc.flowless.VirtualFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import tornadofx.*

class ChatRecvSendView(room: Room): View() {
    override val root = vbox(10.0)

    private val messageInput = TextField()

    init {
        with(root) {
            hgrow = Priority.ALWAYS

            val msgList = room.messageManager.messages
            val virtualList =  VirtualFlow.createVertical(
                    msgList, {create_message_cell(it)}, VirtualFlow.Gravity.REAR
            )
            virtualList.vgrow = Priority.ALWAYS
            virtualList.hgrow = Priority.ALWAYS
            val virtualizedScrollPane = VirtualizedScrollPane<VirtualFlow<RoomMessage, MessageCell>>(virtualList)
            virtualizedScrollPane.vgrow = Priority.ALWAYS
            add(virtualizedScrollPane)

            add(createButtonBar(messageInput))

            add(messageInput)
        }

        with(messageInput) {
            hgrow = Priority.ALWAYS
            action {
                val msg = text
                text = ""
                if (msg.isNotBlank())
                    sendMessage(room.id, msg)
            }
        }
    }
}

private fun createButtonBar(inputField: TextField): ButtonBar {
    val bbar = ButtonBar()
    bbar.apply {
        button {
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PHOTO)
            tooltip("Send image")
            actionEvents()
                    .map {  appState.currRoom.get() }
                    .doOnNext {
                        if (!it.isPresent)
                            alert(Alert.AlertType.WARNING, "No room selected")
                    }
                    .filter{ it.isPresent }
                    .map { it.get() }
                    .addTo(guiEvents.sendImageRequests)
        }
        button{
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SMILE_ALT)
            val ep = EmojiPanel()
            ep.onEmojiChosen = {inputField.text += it.glyph}
            action {
                ep.show(this)
            }
        }
    }
    return bbar
}

