package koma.gui.element.icon

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WeakChangeListener
import javafx.scene.paint.Color
import koma.Server
import koma.network.media.MHUrl
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.icon.avatar.Avatar2L
import mu.KotlinLogging

typealias AvatarUrl = MHUrl
private val logger = KotlinLogging.logger {}

class AvatarAlways(
): StackPane() {
    val avatar = Avatar2L()
    private val name = SimpleStringProperty()
    private var color = Color.BLACK

    init {
        this.add(avatar.root)
        name.addListener { _, _, n: String? ->
            n?:return@addListener
            this.avatar.updateName(n, color)
        }
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
