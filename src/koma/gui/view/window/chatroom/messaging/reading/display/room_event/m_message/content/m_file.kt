package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.chat.FileMessage
import koma.network.media.getFileByMxc
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.launch
import tornadofx.*
import java.io.File

class MFileViewNode(val content: FileMessage): ViewNode {
    override val node: Parent
        get() = HBox(5.0)
    override val menuItems: List<MenuItem>

    var file: File? = null

    init {
        val faicon = guessIconForMime(content.info?.mimetype)
        val icon_node = FontAwesomeIconFactory.get().createIcon(faicon, AppSettings.scale_em(2.0f))

        val fileAvailable = SimpleBooleanProperty(false)

        with(node) {
            add(icon_node)
            label(content.filename)
            setOnMouseClicked { e ->
                if (e.button == MouseButton.PRIMARY) save()
            }
        }

        val mi = MenuItem("Save File")
        with(mi){
            disableWhen { !fileAvailable }
            action { save() }
        }
        menuItems = listOf(mi)

        launch {
            val f = getFileByMxc(content.url)
            if (f != null) {
                file = f
                fileAvailable.set(true)
            }
        }
    }

    fun save() {
        if (file == null)
            alert(Alert.AlertType.ERROR, "File unavailable")
        else {
            saveFileAs(file!!, content.filename)
        }
    }
}

private fun guessIconForMime(mime: String?): FontAwesomeIcon {
    mime?: return FontAwesomeIcon.FILE
    val keyword_icon = mapOf(
            Pair("zip", FontAwesomeIcon.FILE_ZIP_ALT),
            Pair("word", FontAwesomeIcon.FILE_WORD_ALT),
            Pair("video", FontAwesomeIcon.FILE_VIDEO_ALT),
            Pair("text", FontAwesomeIcon.FILE_TEXT),
            Pair("sound", FontAwesomeIcon.FILE_SOUND_ALT),
            Pair("powerpoint", FontAwesomeIcon.FILE_POWERPOINT_ALT),
            Pair("pdf", FontAwesomeIcon.FILE_PDF_ALT),
            Pair("image", FontAwesomeIcon.FILE_IMAGE_ALT),
            Pair("excel", FontAwesomeIcon.FILE_EXCEL_ALT),
            Pair("audio", FontAwesomeIcon.FILE_AUDIO_ALT),
            Pair("archive", FontAwesomeIcon.FILE_ARCHIVE_ALT)
    )
    for ((k,i)in keyword_icon) {
        if (mime.contains(k)) return i
    }
    return FontAwesomeIcon.FILE
}
