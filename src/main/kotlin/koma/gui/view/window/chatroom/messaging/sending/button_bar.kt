package koma.gui.view.window.chatroom.messaging.sending

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.VPos
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import koma.controller.requests.sendFileMessage
import koma.controller.requests.sendImageMessage
import koma.gui.element.emoji.keyboard.EmojiKeyboard
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun createButtonBar(inputField: TextField,
                    currRoom: SimpleObjectProperty<RoomId>
): HBox {
    val bbar = HBox(5.0)
    bbar.apply {
        add(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.FILE, "1.5em").apply {
            fill = Color.FORESTGREEN
            tooltip("Send File")
            setOnMouseClicked {
                currRoom.value?.let {
                    sendFileMessage(room = it)
                }

            }
        })
        add(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PHOTO, "1.5em").apply {
            fill = Color.FORESTGREEN
            tooltip("Send image")
            setOnMouseClicked {
                currRoom.value?.let {
                    sendImageMessage(room = it)
                }
            }
        })
        add(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SMILE_ALT, "1.5em").apply {
            fill = Color.FORESTGREEN
            setOnMouseClicked {
                emojiKeyboard.onEmoji = {
                    logger.trace { "emoji input $it in $it" }
                    inputField.text += it
                }
                emojiKeyboard.show(this)
            }
        })
    }
    return bbar
}

private val emojiKeyboard by lazy { EmojiKeyboard ()}