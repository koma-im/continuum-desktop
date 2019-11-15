package koma.gui.element.icon

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WeakChangeListener
import javafx.scene.image.Image
import javafx.scene.paint.Color
import koma.Server
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.component.FitImageRegion
import link.continuum.desktop.gui.icon.avatar.InitialIcon
import link.continuum.desktop.gui.icon.avatar.UrlAvatar
import link.continuum.desktop.gui.icon.avatar.downloadImageResized
import mu.KotlinLogging
import kotlin.math.max

private val settings: AppSettings = appState.store.settings
val avatarSize: Double by lazy { settings.scaling * 32.0 }
typealias AvatarUrl = MHUrl
private val logger = KotlinLogging.logger {}

class AvatarAlways(
): StackPane(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val avatar = UrlAvatar()
    private val name = SimpleStringProperty()
    private var color = Color.BLACK

    init {
        this.add(avatar.root)
        avatar.root.style = avatarStyle
        name.addListener { _, _, n: String? ->
            n?:return@addListener
            this.avatar.updateName(n, color)
        }
    }
    companion object {
        private val avatarStyle = StyleBuilder().apply {
            val size = 2.em
            fixWidth(size)
            fixHeight(size)
        }.toStyle()
    }

    fun bind(name: ObservableValue<String>, color: Color, url: ObservableValue<AvatarUrl?>,
             server: Server
             ) {
        bindName(name, color)
        bindImage(url, server)
    }

    private fun bindName(name: ObservableValue<String>, color: Color) {
        this.name.unbind()
        this.color = color
        this.name.bind(name)
    }

    private var listener: ChangeListener<AvatarUrl?>? = null

    private fun bindImage(urlV: ObservableValue<AvatarUrl?>, server: Server) {
        fun changeUrl(it: AvatarUrl?) {
            avatar.updateUrl(it, server)
        }
        changeUrl(urlV.value)
        listener = ChangeListener {
            _, _, newValue ->
            changeUrl(newValue)
        }
        urlV.addListener(WeakChangeListener(listener))
    }
}
