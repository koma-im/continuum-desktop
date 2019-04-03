package koma.gui.element.icon.avatar

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.koma_app.appState
import link.continuum.desktop.util.cache.ImgCacheProc
import okhttp3.HttpUrl


object AvatarProvider: ImgCacheProc({ i -> processAvatar(i)}, appState.koma.http.client) {

    fun getAvatar(url: HttpUrl): SimpleObjectProperty<Image> {
         return getImg(url)
    }
}
