package koma.gui.element.icon

import javafx.beans.value.ObservableValue
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.gui.element.icon.avatar.AvatarProvider
import koma.gui.element.icon.placeholder.AvatarPlaceholder
import koma.storage.config.settings.AppSettings
import tornadofx.*


val avatarSize = AppSettings.scaling * 32.0

class AvatarAlways private constructor(urlV: ObservableValue<String>, ap: AvatarPlaceholder): StackPane() {
    constructor(
            urlV: ObservableValue<String>,
            nameV: ObservableValue<String>,
            color: Color
    ): this(
            urlV,
            AvatarPlaceholder(nameV, color))

    constructor(
            urlV: ObservableValue<String>,
            nameV: ObservableValue<String>,
            colorV: ObservableValue<Color>
    ): this(
            urlV,
            AvatarPlaceholder(nameV, colorV))

    private val imageView = ImageView()

    init {
        this.add(ap)

        this.minHeight = avatarSize
        this.minWidth = avatarSize

        updateImage(urlV)
    }


    private fun updateImage(urlV: ObservableValue<String>) {
        val url = urlV.value
        if (url != null) {
            val i = AvatarProvider.getAvatar(url)
            if (i != null) {
                imageView.imageProperty().bind(i)
                this.children.setAll(imageView)
            }
        }
        urlV.addListener { _, _, newValue ->
            newValue ?: return@addListener
            val imgPrp = AvatarProvider.getAvatar(newValue)
            imgPrp ?: return@addListener
            imageView.imageProperty().cleanBind(imgPrp)
            this.children.setAll(imageView)
        }
    }
}
