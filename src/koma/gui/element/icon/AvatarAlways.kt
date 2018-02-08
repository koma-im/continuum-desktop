package koma.gui.element.icon

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
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
        val imageAvl = booleanBinding(imageView.imageProperty()) { value != null }
        this.add(ap)
        ap.removeWhen { imageAvl }
        this.add(imageView)

        this.minHeight = avatarSize
        this.minWidth = avatarSize

        updateImage(urlV)
    }


    private fun updateImage(urlV: ObservableValue<String>) {
        val imp =  urlV.select { url ->
            AvatarProvider.getAvatar(url) ?: SimpleObjectProperty<Image>()
        }
        this.imageView.imageProperty().bind(imp)
    }
}
