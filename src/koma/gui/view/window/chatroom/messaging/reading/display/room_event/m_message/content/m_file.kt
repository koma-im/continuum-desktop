package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.chat.FileMessage
import koma.network.media.MHUrl
import koma.util.result.ok
import koma_app.appState
import tornadofx.*

class MFileViewNode(val content: FileMessage): ViewNode {
    override val node = HBox(5.0)
    override val menuItems: List<MenuItem>
    private val url = MHUrl.fromStr(content.url).ok()

    init {
        val faicon = guessIconForMime(content.info?.mimetype)
        val s = appState.koma.appSettings.scale_em(2f)
        val icon_node = FontAwesomeIconFactory.get().createIcon(faicon,
                s)

        with(node) {
            add(icon_node)
            label(content.filename)
            setOnMouseClicked { e ->
                if (e.button == MouseButton.PRIMARY) save()
            }
        }

        menuItems = createMenuItems()
    }

    private fun createMenuItems(): List<MenuItem> {

        val mi = MenuItem("Save File")
        mi.isDisable = url == null
        mi.action { save() }

        val copyUrl = MenuItem("Copy File Address")
        copyUrl.isDisable = url == null
        copyUrl.action { Clipboard.getSystemClipboard().putString(url.toString()) }

        return listOf(mi, copyUrl)
    }

    private fun save() {
        url?.toHttpUrl()?.ok()?.let {
            downloadFileAs(it, filename = content.filename, title = "Save File As")
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
