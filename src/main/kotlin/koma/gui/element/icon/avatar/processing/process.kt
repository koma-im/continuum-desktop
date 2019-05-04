package koma.gui.element.icon.avatar.processing

import javafx.scene.image.Image
import koma.gui.element.icon.avatarSize
import java.io.InputStream


fun processAvatar(bytes: InputStream): Image {
    val im = Image(bytes, avatarSize, avatarSize, true , true)
    return im
}
