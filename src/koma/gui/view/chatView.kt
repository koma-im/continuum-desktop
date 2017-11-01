package koma.gui.view

import controller.guiEvents
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.gui.view.messagesview.MessagesScrollListView
import koma.gui.view.messagesview.fragment.create_message_cell
import koma.input.emoji.EmojiPanel
import koma_app.appState
import koma.matrix.event.room_message.RoomMessage
import model.Room
import org.fxmisc.flowless.VirtualFlow
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import tornadofx.*

class ChatRecvSendView(room: Room): View() {
    override val root = vbox(10.0)

    val messageListView = MessagesScrollListView(room.messageManager.messages)
    private val messageInput = MessageInputView()

    init {
        with(root) {
            hgrow = Priority.ALWAYS
            //+messageListView
            add(messageListView.root)
            add(createButtonBar(messageInput.root))

            add(messageInput)
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

class MessageInputView(): View() {
    override val root = textfield()

    init {
        with(root) {
            hgrow = Priority.ALWAYS
                actionEvents().map{
                    val msg = text
                    text = ""
                    msg
                }.filter{ it.isNotBlank()
                }.addTo(guiEvents.sendMessageRequests)

        }
    }
}

class MessageListView(msgList: ObservableList<RoomMessage>): View() {
    override val root = VirtualFlow.createVertical(
            msgList, {create_message_cell(it)}, VirtualFlow.Gravity.REAR
    )

    init {
        with(root) {
            vgrow = Priority.ALWAYS
            hgrow = Priority.ALWAYS
        }
    }
}

