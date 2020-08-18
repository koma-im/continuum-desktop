package koma.gui.element.emoji.icon

import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import koma.koma_app.Globals
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.util.cache.ImgCacheProc
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStream
import kotlin.streams.toList

object EmojiCache: ImgCacheProc({ i -> processEmoji(i) }, Globals.httpClient, 9999999) {
    fun getEmoji(emoji: String): ObservableValue<Image> {
        val code = getEmojiCode(emoji)
        val url = getCdnEmojiUrl(code)
        return this.getImg(url)
    }
}

fun getEmojiCode(emoji: String): String {
    val points = emoji.codePoints().filter {
        it != 0xfe0f && it != 0x200d
        && it != 0x2640 && it != 0x2640
    }.toList()
    return points.joinToString("-") { String.format("%x", it) }
}

private fun getCdnEmojiUrl(code: String): HttpUrl {
    return  "https://cdnjs.cloudflare.com/ajax/libs/emojione/2.2.7/assets/png/$code.png".toHttpUrlOrNull()!!
}

private fun processEmoji(
        bytes: InputStream,
        settings: AppSettings = appState.store.settings
): Image {
    val size = settings.fontSize
    val im = Image(bytes, size, size, true , true)
    return im
}
