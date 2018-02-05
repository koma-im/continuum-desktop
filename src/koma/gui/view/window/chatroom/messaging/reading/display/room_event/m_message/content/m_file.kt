package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import koma.matrix.event.room_message.chat.FileMessage
import koma.network.media.getFileByMxc
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.launch
import tornadofx.*
import java.io.File

fun m_file(content: FileMessage): Node {
    val faicon = guessIconForMime(content.info?.mimetype)
    val icon_node = FontAwesomeIconFactory.get().createIcon(faicon, AppSettings.scale_em(2.0f))

    val fileAvailable = SimpleBooleanProperty(false)
    var file: File? = null

    fun save() {
        if (file == null)
            alert(Alert.AlertType.ERROR, "File unavailable")
        else {
            saveFileAs(file!!, content.filename)
        }
    }

    val box = HBox(5.0)
    with(box) {
        add(icon_node)
        label(content.filename)
        setOnMouseClicked { e ->
            if (e.button == MouseButton.PRIMARY) save()
        }
    }

    launch {
        val f = getFileByMxc(content.url)
        if (f != null) {
            file = f
            fileAvailable.set(true)
        }
    }
    return box
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
