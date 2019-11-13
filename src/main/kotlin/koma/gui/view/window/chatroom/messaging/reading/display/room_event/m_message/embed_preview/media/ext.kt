package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media

import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.VideoElement
import koma.koma_app.appState
import koma.network.media.MHUrl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.util.http.MediaServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

@ExperimentalCoroutinesApi
class MediaViewers(private val server: MediaServer) {
    private val imageViewer by lazy { ImageElement()  }
    private val imageExts = setOf("jpg", "jpeg", "gif", "png")
    private val videoExts = setOf("mp4")
    fun get(ext: String, url: HttpUrl): ViewNode? {
        if (ext in imageExts) return imageViewer.apply { update(MHUrl.Http(url), server) }
        if (ext in videoExts) return VideoElement(url)
        return null
    }
}
