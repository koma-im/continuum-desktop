package koma.gui.view

import javafx.collections.ObservableList
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import koma.Failure
import koma.controller.requests.membership.runAskBanRoomMember
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.window.preferences.PreferenceWindow
import koma.gui.view.window.roomfinder.RoomFinder
import koma.gui.view.window.userinfo.actions.chooseUpdateUserAvatar
import koma.gui.view.window.userinfo.actions.updateMyAlias
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.Account
import model.Room
import org.controlsfx.control.NotificationPane

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
    private val prefWin by lazy { PreferenceWindow() }
    init {
        with(content) {
            background = whiteBackGround
            center = this@ChatWindowBars.center.root
            top = MenuBar().apply {
                menu("File") {
                    item("Create Room").action { createRoomInteractive() }
                    item("Join Room") {
                        setOnAction { roomFinder.open() }
                    }
                    item("Preferences").action {
                        prefWin.openModal(owner = JFX.primaryStage)
                    }
                    item("Quit").action {
                        JFX.primaryStage.close()
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
                contextMenu = ContextMenu(
                        MenuItem("Update my avatar").apply { setOnAction { chooseUpdateUserAvatar() } },
                        MenuItem("Update my name").apply { setOnAction {  updateMyAlias() }},
                        MenuItem("Join Room").apply { setOnAction { roomFinder.open() }}
                )
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
