package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import koma.gui.element.icon.AvatarAlways
import koma.koma_app.appState
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.switchUpdates
import mu.KotlinLogging
import okhttp3.OkHttpClient
import tornadofx.*

private val logger = KotlinLogging.logger {}

fun showUser(node: Node, userId: UserId) {
    val user = appState.store.userStore.getOrCreateUserId(userId)
    node.apply {
        hbox(spacing = 5.0) {
            add(AvatarAlways(user.avatar, SimpleStringProperty(userId.str), user.color))
            vbox {
                alignment = Pos.CENTER
                label(userId.str) {
                    minWidth = 50.0
                    maxWidth = 100.0
                    textFill = user.color
                }
            }
        }
    }
}

/**
 * view of user avatar and name when showing a state change event
 */
@ExperimentalCoroutinesApi
class StateEventUserView(private val store: UserDataStore,
                         private val client: OkHttpClient,
                         avatarSize: Double) {
    val root = HBox(5.0)
    private val avatarView = AvatarView(store, client, avatarSize)
    private val nameLabel: Label
    private val itemId = ConflatedBroadcastChannel<UserId>()
    fun updateUser(userId: UserId) {
        if (!itemId.offer(userId)) {
            logger.error { "$userId not offered" }
        }
    }
    init {
        root.add(avatarView.root)
        val l = VBox().apply {
            alignment = Pos.CENTER
            nameLabel = label() {
                minWidth = 50.0
                maxWidth = 100.0
            }
        }
        root.add(l)

        GlobalScope.launch {
            val newName = switchUpdates(itemId.openSubscription()) { store.getNameUpdates(it) }
            for (n in newName) {
                withContext(Dispatchers.JavaFx) {
                    nameLabel.text = n

                }
            }
        }
        GlobalScope.launch {
            for (id in itemId.openSubscription()) {
                avatarView.updateUser(id)
                nameLabel.textFill = store.getUserColor(id)
            }
        }
    }
}
