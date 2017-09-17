package koma.gui.view.messagesview.fragment

import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.media.getMxcImagePropery
import model.EmoteMsg
import model.ImageMsg
import model.MsgType
import model.TextMsg
import tornadofx.*

fun MsgType.render_node(): Node {
    val node = TextFlow()
    when(this) {
        is TextMsg -> {
            node.add(Text(this.text))
        }
        is EmoteMsg -> {
            node.add(Text(this.text))
        }
        is ImageMsg -> {
            val im = ImageView()
            im.tooltip(this.desc)
            im.imageProperty().bind(getMxcImagePropery(this.mxcurl, 320.0, 120.0))
            node.add(im)
        }
    }
    return node
}
