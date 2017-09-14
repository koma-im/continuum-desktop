package koma.gui.view

import controller.guiEvents
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import koma.concurrency.runTask
import koma_app.appState
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import rx.javafx.kt.toObservable
import tornadofx.*
import view.MessageFragment
import view.WidthModel
import view.popup.EmojiData
import view.popup.EmojiPanel

class ChatMainView(): View() {
    override val root = vbox(10.0)

    val messageListView: MessageListView by inject()
    private val messageInput: MessageInputView by inject()

    init {
        with(root) {
            hgrow = Priority.ALWAYS
            //+messageListView
            add(messageListView)
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
            runTask({ EmojiData.getSmileEmoji()},
                    {
                        val iv = ImageView(it)
                        iv.fitHeight = 12.0
                        iv.isPreserveRatio = true
                        graphic = iv })
            action {
                val ep = EmojiPanel(inputField)
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

class MessageListView(): View() {
    override val root = listview(appState.currChatMessageList)

    init {
        with(root) {
            itemsProperty().toObservable().subscribe {
                this.scrollTo(it.size - 1)
            }
            vgrow = Priority.ALWAYS
            hgrow = Priority.ALWAYS
            fixedCellSize = -1.0
            cellFragment(MessageFragment::class)
            setInScope(WidthModel(this.widthProperty()), scope)
        }
    }
}

