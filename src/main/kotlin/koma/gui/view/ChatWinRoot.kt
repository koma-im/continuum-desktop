package koma.gui.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import koma.Failure
import koma.Koma
import koma.controller.requests.membership.dialogInviteMember
import koma.controller.requests.membership.runAskBanRoomMember
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.window.preferences.PreferenceWindow
import koma.gui.view.window.roomfinder.RoomFinder
import koma.gui.view.window.userinfo.actions.chooseUpdateUserAvatar
import koma.gui.view.window.userinfo.actions.updateMyAlias
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.KDataStore
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.Account
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.controlsfx.control.NotificationPane
import org.controlsfx.control.action.Action
import tornadofx.*

private val settings: AppSettings = appState.store.settings

/**
 * everything inside the app window after login
 * including a menu bar at the top
 *
 * Created by developer on 2017/6/17.
 */
class ChatWindowBars(
        roomList: ObservableList<Room>,
        account: Account,
        store: AppStore
) {
    private val content = BorderPane()
    val root = NotificationPane(content)
    val center = ChatView(roomList, account, store)
    val status = SyncStatusBar(root)

    private val roomFinder by lazy { RoomFinder(account) }
    init {
        with(content) {
            style {
                backgroundColor = multi(Color.WHITE)
                fontSize= settings.scaling.em
            }
            center = this@ChatWindowBars.center.root
            top = menubar {
                menu("File") {
                    item("Create Room").action { createRoomInteractive() }
                    item("Join Room") {
                        action { roomFinder.open() }
                    }
                    item("Preferences").action {
                        find(PreferenceWindow::class).openModal()
                    }
                    item("Quit").action {
                        FX.primaryStage.close()
                    }
                }
                menu("Room") {
                    item("Ban Member") {
                        action { runAskBanRoomMember() }
                    }
                }
                menu("Me") {
                    item("Update avatar").action { chooseUpdateUserAvatar() }
                    item("Update my name").action { updateMyAlias() }
                }
                contextmenu {
                    item("Update my avatar").action { chooseUpdateUserAvatar() }
                    item("Update my name").action { updateMyAlias() }
                    item("Join Room").action { roomFinder.open() }
                }
            }
        }
    }
}

class SyncStatusBar(
        private val pane: NotificationPane
): CoroutineScope by CoroutineScope(Dispatchers.Main) {
    val ctrl = Channel<Variants>(Channel.CONFLATED)

    init {
        launch {
            for (s in ctrl) {
                update(s)
            }
        }
    }

    private fun update(s: Variants) {
        when (s) {
            is Variants.Normal -> {
                pane.hide()
            }
            is Variants.NeedRetry -> {
                pane.text = "Connecting"
                if (!pane.isShowing) {
                    pane.show()
                }
            }
        }
    }

    // various states
    sealed class Variants {
        class Normal(): Variants()
        // network issue that may be temporary
        class NeedRetry(val err: Failure): Variants()
    }
}
