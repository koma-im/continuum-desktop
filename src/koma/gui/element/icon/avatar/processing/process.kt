package koma.gui.element.icon.avatar.processing

import javafx.scene.image.Image
import koma.storage.config.settings.AppSettings
import java.io.InputStream

private val avsize = AppSettings.scaling * 32.0

fun processAvatar(bytes: InputStream): Image {
    val im = Image(bytes, avsize, avsize, true , true)
    return im
}
