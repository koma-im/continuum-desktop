package koma.gui.view.usersview.fragment

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.icon.avatar.SelectUser
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.switchUpdates
import mu.KotlinLogging
import okhttp3.OkHttpClient
import tornadofx.*

private val logger = KotlinLogging.logger {}
@ExperimentalCoroutinesApi
private val settings: AppSettings = appState.store.settings

@ExperimentalCoroutinesApi
class MemberCell(
        private val showNoName: SimpleBooleanProperty,
        private val store: UserDataStore,
        client: OkHttpClient,
        scale: Float = settings.scaling,
        avsize: Double = scale * 32.0
) : ListCell<SelectUser>() {
    private val root = HBox( 5.0)
    private val toolTip = Tooltip()
    private val avatarView = AvatarView(store, avsize)
    private val name: Label

    private val itemId = ConflatedBroadcastChannel<SelectUser>()
    init {

        root.apply {
            tooltip = toolTip
            minWidth = 1.0
            prefWidth = 1.0
            style {
                alignment = Pos.CENTER_LEFT
                fontSize= scale.em
            }
            stackpane {
                add(avatarView.root)
                minHeight = avsize
                minWidth = avsize
            }

            name = label() {
                whenVisible {  }
                removeWhen(showNoName)
            }
        }

        GlobalScope.launch {
            val newName = switchUpdates(itemId.openSubscription()) { store.getNameUpdates(it.first) }
            for (n in newName) {
                withContext(Dispatchers.JavaFx) {
                    logger.debug { "updating name in user list: ${itemId.valueOrNull} is $n" }
                    name.text = n
                }
            }
        }
        GlobalScope.launch {
            for (id in itemId.openSubscription()) {
                avatarView.updateUser(id.first, id.second)
                toolTip.text = id.first.str
            }
        }
    }

    override fun updateItem(item: SelectUser?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        itemId.offer(item)
        name.textFill = store.getUserColor(item.first)

        graphic = root
    }
}
