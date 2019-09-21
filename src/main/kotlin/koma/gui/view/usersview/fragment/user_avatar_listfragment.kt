package koma.gui.view.usersview.fragment

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import koma.Server
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private typealias SelectUser = Pair<UserId, Server>

@ExperimentalCoroutinesApi
class MemberCell(
        private val showNoName: SimpleBooleanProperty,
        private val store: UserDataStore
) : ListCell<SelectUser>() {
    private val root = HBox( 5.0)
    private val toolTip = Tooltip()
    private val avatarView = AvatarView(store)
    private val name: Label

    private val itemId = ConflatedBroadcastChannel<SelectUser>()
    init {

        root.apply {
            tooltip = toolTip
            minWidth = 1.0
            prefWidth = 1.0
            alignment = Pos.CENTER_LEFT
            add(avatarView.root)

            name = label() {
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
