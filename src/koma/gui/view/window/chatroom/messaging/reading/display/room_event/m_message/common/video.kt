package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.scene.control.Button
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.storage.config.settings.AppSettings
import okhttp3.HttpUrl
import tornadofx.*

class VideoElement(val url: HttpUrl): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    private val mediaView = MediaView()
    init {
        val playButton = Button()
        playButton.graphic = MaterialIconFactory.get().createIcon(
                MaterialIcon.PLAY_ARROW,
                AppSettings.scale_em(3.0f))
        playButton.action {
            node.children.remove(playButton)
            playVideo()
        }

        mediaView.setOnError { e->
            e.mediaError.printStackTrace()
            //println("media error ${e.mediaError}")
        }
        mediaView.fitWidth = 200.0 * AppSettings.settings.scaling

        node.minHeight = 100.0 * AppSettings.settings.scaling
        node.add(mediaView)
        node.add(playButton)

        menuItems = listOf()
    }

    private fun playVideo() {
        val media = Media(url.toString())
        val player = MediaPlayer(media)
        player.isAutoPlay = false
        mediaView.mediaPlayer = player
        player.play()
    }
}

