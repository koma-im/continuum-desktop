package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.control.Label
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * view of user avatar and name when showing a state change event
 */
@ExperimentalCoroutinesApi
class StateEventUserView(private val store: UserDataStore) {
    val root = HBox(5.0)
    private val avatarView = AvatarView(store)
    private val nameLabel: Label
    private val itemId = ConflatedBroadcastChannel<Pair<UserId, Server>>()
    fun updateUser(userId: UserId, mediaServer: MediaServer) {
        if (!itemId.offer(userId to mediaServer)) {
            logger.error { "$userId not offered" }
        }
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

        GlobalScope.launch {
            val newName = switchUpdates(itemId.openSubscription()) { store.getNameUpdates(it.first) }
            for (n in newName) {
                withContext(Dispatchers.JavaFx) {
                    nameLabel.text = n

                }
            }
        }
        GlobalScope.launch {
            for (id in itemId.openSubscription()) {
                avatarView.updateUser(id.first, id.second)
                nameLabel.textFill = store.getUserColor(id.first)
            }
        }
    }
}
