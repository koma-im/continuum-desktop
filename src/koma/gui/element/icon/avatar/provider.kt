package koma.gui.element.icon.avatar

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.network.matrix.media.makeAnyUrlHttp
import koma.network.media.ImgCacheProc


object AvatarProvider: ImgCacheProc({i -> processAvatar(i)}) {

    fun getAvatar(uri: String): SimpleObjectProperty<Image>? {
        val url = makeAnyUrlHttp(uri)
        url ?: return null
        return getProcImg(url)
    }
}
