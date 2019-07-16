package koma.gui.element.icon

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WeakChangeListener
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.Koma
import koma.Server
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.icon.avatar.InitialIcon
import link.continuum.desktop.gui.icon.avatar.downloadImageResized
import mu.KotlinLogging
import okhttp3.HttpUrl
import tornadofx.*

private val settings: AppSettings = appState.store.settings
val avatarSize: Double by lazy { settings.scaling * 32.0 }
typealias AvatarUrl = MHUrl
private val logger = KotlinLogging.logger {}


class AvatarAlways(
        private val koma: Koma,
        private val avatarSize: Double = settings.scaling * 32.0
): StackPane() {
    private val initialIcon = InitialIcon(avatarSize)
    private val imageView = ImageView()
    private val name = SimpleStringProperty()
    private var color = Color.BLACK

    init {
        val imageAvl = booleanBinding(imageView.imageProperty()) { value != null }
        this.add(initialIcon.root)
        initialIcon.root.removeWhen { imageAvl }
        this.add(imageView)

        this.minHeight = avatarSize
        this.minWidth = avatarSize
        name.addListener { _, _, n: String? ->
            n?:return@addListener
            this.initialIcon.updateItem(n, color)
        }
    }

    fun bind(name: ObservableValue<String>, color: Color, url: ObservableValue<AvatarUrl?>,
             server: Server
             ) {
        bindName(name, color)
        bindImage(url, server)
    }

    fun bindName(name: ObservableValue<String>, color: Color) {
        this.name.unbind()
        this.color = color
        this.name.bind(name)
    }

    private var listener: ChangeListener<AvatarUrl?>? = null

    fun bindImage(urlV: ObservableValue<AvatarUrl?>, server: Server) {
        val im = SimpleObjectProperty<Image>()
        fun changeUrl(it: AvatarUrl?) {
            im.unbind()
            im.value = null
            logger.debug { "new avatar url $it" }
            it ?: return
            im.bind(downloadImageResized(it, avatarSize, server))
        }
        changeUrl(urlV.value)
        listener = ChangeListener {
            _, _, newValue ->
            changeUrl(newValue)
        }
        urlV.addListener(WeakChangeListener(listener))
        this.imageView.imageProperty().unbind()
        this.imageView.imageProperty().bind(im)
    }
}
