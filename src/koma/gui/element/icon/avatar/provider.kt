package koma.gui.element.icon.avatar

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.network.media.ImgCacheProc
import koma.network.media.MHUrl


object AvatarProvider: ImgCacheProc({i -> processAvatar(i)}) {

    fun getAvatar(uri: String): Result<SimpleObjectProperty<Image>, Exception>
            = MHUrl.fromStr(uri).map { getProcImg(it) }

}
