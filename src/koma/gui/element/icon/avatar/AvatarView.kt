package koma.gui.element.icon.avatar

import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.scene.image.ImageView
import okhttp3.HttpUrl
import tornadofx.*

class AvatarView private constructor(): ImageView() {

    val imageAvailable = booleanBinding(this.imageProperty()) { value != null }

    constructor(url: HttpUrl): this() {
        AvatarProvider.getAvatar(url).let {  this.imageProperty().bind(it) }
    }

    constructor(urlProperty: ObservableValue<HttpUrl>): this() {
        urlProperty.addListener{_: Observable ->
            val a = urlProperty.value?.let {AvatarProvider.getAvatar(it) }
            a ?: return@addListener
            a.let { this.imageProperty().cleanBind(it) }
        }
    }
}

