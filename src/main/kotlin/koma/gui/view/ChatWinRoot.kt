package koma.gui.view

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import koma.Failure
import koma.controller.requests.membership.runAskBanRoomMember
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.window.preferences.PreferenceWindow
import koma.gui.view.window.roomfinder.RoomFinder
import koma.gui.view.window.userinfo.actions.chooseUpdateUserAvatar
import koma.gui.view.window.userinfo.actions.updateMyAlias
import koma.koma_app.AppData
import koma.koma_app.AppStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import link.continuum.desktop.action.SyncControl
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.view.AccountContext
import link.continuum.desktop.util.Account
import mu.KotlinLogging
import org.controlsfx.control.NotificationPane

private val logger = KotlinLogging.logger {}

/**
 * everything inside the app window after login
 * including a menu bar at the top
 *
 * Created by developer on 2017/6/17.
 */
class ChatWindowBars(
        account: Account,
        keyValueStore: KeyValueStore,
        parentJob: Job,
        appData: Deferred<AppData>
) {
    private val scope = CoroutineScope(SupervisorJob(parentJob) + Dispatchers.Main)
    private val context = AccountContext(account)
    private val content = BorderPane()
    val root = NotificationPane(content)
    val center: ChatView
    private val roomFinder = scope.async(start = CoroutineStart.LAZY) {
        logger.error { "opening room directory" }
        val store = appData.await()
        RoomFinder(account, store)
    }
    private val prefWin by lazy { PreferenceWindow(keyValueStore.proxyList) }
    val syncControl: SyncControl
    init {
        val roomList = keyValueStore.roomsOf(account.userId)
        center = ChatView( roomList.joinedRoomList, context, appData)
        syncControl = SyncControl(
                account,
                appData,
                parentJob = parentJob,
                view = center
        )
        with(content) {
            background = whiteBackGround
            center = this@ChatWindowBars.center.root
            top = MenuBar().apply {
                menu("File") {
                    item("Create Room").action { createRoomInteractive() }
                    item("Join Room") {
                        setOnAction { openRoomDirectory() }
                    }
                    item("Preferences").action {
                        prefWin.openModal(owner = JFX.primaryStage)
                    }
                    item("Sign out").action {
                        keyValueStore.activeAccount.remove()
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
                        MenuItem("Join Room").apply { setOnAction { openRoomDirectory() }}
                ).apply {
                    menu("Debug") {
                        item("Force sync") {
                            action {
                                syncControl.restartInitial()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openRoomDirectory() {
        scope.launch {
            roomFinder.await().open()
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
                @Suppress("ControlFlowWithEmptyBody")
                if (!pane.isShowing) {
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
