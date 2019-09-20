package koma.gui.element.icon

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WeakChangeListener
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import koma.Koma
import koma.Server
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.InitialIcon
import link.continuum.desktop.gui.icon.avatar.downloadImageResized
import mu.KotlinLogging
import kotlin.math.max

private val settings: AppSettings = appState.store.settings
val avatarSize: Double by lazy { settings.scaling * 32.0 }
typealias AvatarUrl = MHUrl
private val logger = KotlinLogging.logger {}

class AvatarAlways(
): StackPane(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val initialIcon = InitialIcon()
    private val name = SimpleStringProperty()
    private var color = Color.BLACK

    init {
        val imageAvl = booleanBinding(backgroundProperty()) { value != null }
        this.add(initialIcon.root)
        initialIcon.root.removeWhen(imageAvl)
        style {
            prefWidth = 2.em()
            prefHeight = 2.em()
        }
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
            im.bind(downloadImageResized(it, max(this.width, 32.0), server))
        }
        changeUrl(urlV.value)
        listener = ChangeListener {
            _, _, newValue ->
            changeUrl(newValue)
        }
        urlV.addListener(WeakChangeListener(listener))
        val bg = objectBinding(im) {
            val image = this.value ?: return@objectBinding null
            Background(BackgroundImage(image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    bgSize
            ))
        }
        this.backgroundProperty().cleanBind(bg)
    }
    companion object {
        private val bgSize =  BackgroundSize(100.0, 100.0,
                true, true,
                false, true)
    }
}
