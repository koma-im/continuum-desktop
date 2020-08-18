package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.scene.control.Button
import javafx.scene.control.MenuItem
import link.continuum.desktop.gui.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.action
import link.continuum.desktop.gui.add
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

private val settings: AppSettings = appState.store.settings

class VideoElement(val url: HttpUrl): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    private val mediaView = MediaView()
    init {
        val playButton = Button()
        val s = settings.scale_em(3.0f)
        playButton.graphic = MaterialIconFactory.get().createIcon(
                MaterialIcon.PLAY_ARROW,
                s)
        playButton.action {
            node.children.remove(playButton)
            playVideo()
        }

        mediaView.setOnError { e->
            e.mediaError.printStackTrace()
            //println("media error ${e.mediaError}")
        }
        val s1 = settings.scaling
        mediaView.fitWidth = 200.0 * s1

        node.minHeight = 100.0 * s1
        node.add(mediaView)
        node.add(playButton)

        menuItems = listOf()
    }

    private fun playVideo() {
        runCatching {
            val media = Media(url.toString())
            val player = MediaPlayer(media)
            player.isAutoPlay = false
            mediaView.mediaPlayer = player
            player.play()
        }.onFailure {
            logger.warn { "media $url causes error $it" }
        }
    }
}

