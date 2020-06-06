package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.control.Label
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * view of user avatar and name when showing a state change event
 */
@ExperimentalCoroutinesApi
class StateEventUserView(
        private val store: UserDataStore
) {
    private val scope = MainScope()
    val root = HBox(5.0)
    private val avatarView = AvatarView(store)
    private val nameLabel: Label
    private val itemId = MutableObservable<Pair<UserId, MediaServer>>()
    fun updateUser(userId: UserId, mediaServer: MediaServer) {
        itemId.set(value = userId to mediaServer)
    }
    init {
        root.add(avatarView.root)
        val l = VBox().apply {
            alignment = Pos.CENTER
            nameLabel = label {
                minWidth = 50.0
                maxWidth = 100.0
            }
        }
        root.add(l)

        itemId.flow()
                .flatMapLatest {
            store.getNameUpdates(it.first)
        }.onEach {
            nameLabel.text = it
        }.launchIn(scope)
        itemId.flow()
                .onEach {
            avatarView.updateUser(it.first, it.second)
            nameLabel.textFill = store.getUserColor(it.first)
        }.launchIn(scope)
    }
}
