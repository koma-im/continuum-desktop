package koma.gui.view.window.chatroom.messaging.sending

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import koma.controller.requests.sendFileMessage
import koma.controller.requests.sendImageMessage
import koma.gui.element.emoji.keyboard.EmojiKeyboard
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import model.Room
import mu.KotlinLogging
import tornadofx.*
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

fun createButtonBar(inputField: TextField, room: Room,
                    settings: AppSettings = appState.store.settings
                    ): ButtonBar {
    val bbar = ButtonBar()
    val scale = settings.scaling
    val size = "${scale.roundToInt()}em"
    bbar.apply {
        style {
            fontSize = scale.em
        }
        button {
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.FILE, size)
            tooltip("Send File")
            action { sendFileMessage(room = room.id) }
        }
        button {
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PHOTO, size)
            tooltip("Send image")
            action { sendImageMessage(room = room.id) }
        }
        button{
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SMILE_ALT, size)
            action {
                emojiKeyboard.onEmoji = {
                    logger.trace { "emoji input $it in $it" }
                    inputField.text += it
                }
                emojiKeyboard.show(this)
            }
        }
    }
    return bbar
}

private val emojiKeyboard by lazy { EmojiKeyboard ()}