package koma.gui.element.emoji.icon

import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.util.cache.ImgCacheProc
import okhttp3.HttpUrl
import java.io.InputStream
import kotlin.streams.toList

object EmojiCache: ImgCacheProc({ i -> processEmoji(i) }) {
    fun getEmoji(emoji: String): ObservableValue<Image> {
        val code = getEmojiCode(emoji)
        val url = getCdnEmojiUrl(code)
        return getProcImg(MHUrl.Http(url,365 * 86400))
    }
}

fun getEmojiCode(emoji: String): String {
    val points = emoji.codePoints().filter {
        it != 0xfe0f && it != 0x200d
        && it != 0x2640 && it != 0x2640
    }.toList()
    return points.map { String.format("%x", it) }.joinToString("-")
}

private fun getCdnEmojiUrl(code: String): HttpUrl {
    return  HttpUrl.parse("https://cdnjs.cloudflare.com/ajax/libs/emojione/2.2.7/assets/png/$code.png")!!
}

private fun processEmoji(
        bytes: InputStream,
        settings: AppSettings = appState.store.settings
): Image {
    val size = settings.fontSize
    val im = Image(bytes, size, size, true , true)
    return im
}
