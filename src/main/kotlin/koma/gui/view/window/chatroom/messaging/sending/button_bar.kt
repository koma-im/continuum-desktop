package koma.gui.view.window.chatroom.messaging.sending

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import koma.controller.requests.sendFileMessage
import koma.controller.requests.sendImageMessage
import koma.gui.element.emoji.keyboard.EmojiKeyboard
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.action
import link.continuum.desktop.gui.button
import link.continuum.desktop.gui.tooltip
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun createButtonBar(inputField: TextField,
                    currRoom: SimpleObjectProperty<RoomId>,
                    settings: AppSettings = appState.store.settings
                    ): ButtonBar {
    val bbar = ButtonBar()
    val scale = settings.scaling
    bbar.apply {
        button {
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.FILE)
            tooltip = Tooltip("Send File")
            setOnAction {
                currRoom.value?.let {
                    sendFileMessage(room = it)
                }

            }
        }
        button {
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PHOTO)
            tooltip("Send image")
            setOnAction {
                currRoom.value?.let {
                    sendImageMessage(room = it)
                }
            }
        }
        button{
            graphic = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SMILE_ALT)
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