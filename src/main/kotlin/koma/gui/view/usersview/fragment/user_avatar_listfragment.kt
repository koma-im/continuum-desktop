package koma.gui.view.usersview.fragment

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tooltip
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.label
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.observable.MutableObservable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private typealias SelectUser = Pair<UserId, Server>

@ExperimentalCoroutinesApi
class MemberCell(
        private val store: UserDataStore
) : ListCell<SelectUser>() {
    private val scope = MainScope()
    private val root = HBox( 5.0)
    private val toolTip = Tooltip()
    private val avatarView = AvatarView(store)
    private val name: Label

    private val itemId = MutableObservable<SelectUser>()
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
        itemId.flow().flatMapLatest {
            store.getNameUpdates(it.first)
        }.onEach {
            logger.debug { "updating name in user list: ${itemId.getOrNull()} is $it" }
            check(Platform.isFxApplicationThread())
            name.text = it
        }.launchIn(scope)
        itemId.flow().onEach { id ->
            avatarView.updateUser(id.first, id.second)
            toolTip.text = id.first.str
        }.launchIn(scope)
    }

    override fun updateItem(item: SelectUser?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        itemId.set(item)
        name.textFill = store.getUserColor(item.first)

        graphic = root
    }
}
