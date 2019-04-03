package koma.gui.element.icon

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.gui.element.icon.avatar.AvatarProvider
import koma.gui.element.icon.placeholder.AvatarPlaceholder
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import mu.KotlinLogging
import okhttp3.HttpUrl
import tornadofx.*

private val settings: AppSettings = appState.store.settings
val avatarSize by lazy { settings.scaling * 32.0 }
typealias AvatarUrl = HttpUrl
private val logger = KotlinLogging.logger {}

class AvatarAlways private constructor(urlV: ObservableValue<AvatarUrl>, ap: AvatarPlaceholder): StackPane() {
    constructor(
            urlV: ObservableValue<AvatarUrl>,
            nameV: ObservableValue<String>,
            color: Color
    ): this(
            urlV,
            AvatarPlaceholder(nameV, color))

    constructor(
            urlV: ObservableValue<AvatarUrl>,
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


    private fun updateImage(urlV: ObservableValue<AvatarUrl>) {
        val imp =  urlV.select { url: HttpUrl? ->
            if (url != null) {
                AvatarProvider.getAvatar(url)
            } else   {
                logger.debug { "no avatar url" }
                SimpleObjectProperty()
            }
        }
        this.imageView.imageProperty().bind(imp)
    }
}
