package koma.gui.element.emoji.icon

import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import koma.network.media.ImgCacheProc
import koma.storage.config.settings.AppSettings
import okhttp3.HttpUrl
import java.io.InputStream

object EmojiCache: ImgCacheProc({ i -> processEmoji(i) }) {
    fun getEmoji(emojicode: String): ObservableValue<Image> {
        val url = getCdnEmojiUrl(emojicode)
        return getProcImg(url, 365)
    }
}

private fun getCdnEmojiUrl(code: String): HttpUrl {
    return  HttpUrl.parse("https://cdnjs.cloudflare.com/ajax/libs/emojione/2.2.7/assets/png/$code.png")!!
}

private fun processEmoji(bytes: InputStream): Image {
    val size = AppSettings.fontSize
    val im = Image(bytes, size, size, true , true)
    return im
}
