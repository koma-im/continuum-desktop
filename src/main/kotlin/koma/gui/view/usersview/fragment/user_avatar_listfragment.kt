package koma.gui.view.usersview.fragment

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tooltip
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.label
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.switchUpdates
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private typealias SelectUser = Pair<UserId, Server>

@ExperimentalCoroutinesApi
class MemberCell(
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

            name = label().apply {
                ellipsisString = ""
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
        GlobalScope.launch(Dispatchers.Main) {
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
