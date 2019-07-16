package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media

import koma.Koma
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.VideoElement
import koma.koma_app.appState
import koma.network.media.MHUrl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.HttpUrl

@ExperimentalCoroutinesApi
class MediaViewers(koma: Koma) {
    private val imageViewer by lazy { ImageElement(koma)  }
    private val imageExts = setOf("jpg", "jpeg", "gif", "png")
    private val videoExts = setOf("mp4")
    fun get(ext: String, url: HttpUrl): ViewNode? {
        if (ext in imageExts) return imageViewer.apply { update(url) }
        if (ext in videoExts) return VideoElement(url)
        return null
    }
}
