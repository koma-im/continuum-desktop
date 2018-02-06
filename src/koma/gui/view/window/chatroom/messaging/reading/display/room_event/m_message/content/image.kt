package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import javafx.stage.FileChooser
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.media.downloadImageUri
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.launch
import tornadofx.*
import java.io.File

class MImageViewNode(val content: ImageMessage): ViewNode {
    override val node = ImageView()
    override val menuItems: List<MenuItem>

    var file: File? = null

    init {
        val imageView = node
        imageView.isPreserveRatio = true
        imageView.tooltip(content.body)
        val scale = AppSettings.settings.scaling
        imageView.fitWidth = 320.0 * scale
        imageView.fitHeight = 320.0 * scale
        imageView.isSmooth = true

        val imageAvailable = SimpleBooleanProperty(false)

        val tm = MenuItem("Save Image")
        with(tm) {
            disableWhen { !imageAvailable }
            action { save() }
        }
        menuItems = listOf(tm)

        launch {
            val f = downloadImageUri(content.url)
            if (f != null) {
                imageAvailable.set(true)
                imageView.image = f
            }
        }
    }

    fun save() {
        if (file == null)
            alert(Alert.AlertType.ERROR, "Image file unavailable")
        else {
            saveFileAs(file!!, content.body)
        }
    }
}

fun saveFileAs(image: File, name: String) {
    val dialog = FileChooser()
    dialog.title = "Save image as"
    dialog.initialFileName = name

    val file = dialog.showSaveDialog(FX.primaryStage)
    file?:return

    image.copyTo(file, true)
}
