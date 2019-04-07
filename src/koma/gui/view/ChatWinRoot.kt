package koma.gui.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.controller.requests.membership.ask_invite_member
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
import link.continuum.desktop.database.KDataStore
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

private val settings: AppSettings = appState.store.settings

/**
 * everything inside the app window after login
 * including a menu bar at the top
 *
 * Created by developer on 2017/6/17.
 */
class ChatWindowBars(
        roomList: ObservableList<Room>, server: HttpUrl, kDataStore: KDataStore,
        store: AppStore,
        httpClient: OkHttpClient
) {
    val root = BorderPane()
    // used to show sync errors and allow user intervention
    val statusBar = VBox()

    init {
        with(root) {
            style {
                fontSize= settings.scaling.em
            }
            center = ChatView(roomList, server, kDataStore, store, httpClient).root
            top = menubar {
                menu("File") {
                    item("Create Room").action { createRoomInteractive() }
                    item("Join Room") {
                        action { RoomFinder(server).open() }
                    }
                    item("Preferences").action {
                        find(PreferenceWindow::class).openModal()
                    }
                    item("Quit").action {
                        FX.primaryStage.close()
                    }
                }
                menu("Room") {
                    item("Invite Member"){
                        action { ask_invite_member() }
                    }
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
                }
            }
            bottom = statusBar
        }
    }
}

class SyncStatusBar() {
    val root = HBox()
    val status = Channel<Variants>(Channel.CONFLATED)
    private val hideStatus = SimpleBooleanProperty(true)
    private val hideButton = SimpleBooleanProperty(true)
    private val text = SimpleStringProperty()
    private val button = Button()
    init {
        root.removeWhen(hideStatus)
        button.removeWhen(hideButton)
        with(root) {
            label(text)
            stackpane { hgrow = Priority.ALWAYS }
            add(button)
        }
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in status) {
                update(s)
            }
        }
    }

    private fun update(s: Variants) {
        when (s) {
            is Variants.Normal -> {
                hideStatus.set(true)
            }
            is Variants.FullSync -> {
                hideButton.set(true)
                text.set("Doing a full sync, it may take several seconds")
                hideStatus.set(false)
            }
            is Variants.NeedRetry -> {
                button.text = "Retry Now"
                hideButton.set(false)
                val countDown = GlobalScope.launch(Dispatchers.JavaFx) {
                    for (i in 9 downTo 1) {
                        text.set("Network issue, retrying in $i seconds")
                        delay(1000)
                    }
                    setRetrying(s.retryNow)
                }
                button.setOnAction {
                    countDown.cancel()
                    setRetrying(s.retryNow)
                }
                hideStatus.set(false)
            }
        }
    }

    /**
     * one issue is that the status is only set to normal when there are new events
     * when there are not a lot going on, it may appear to be waiting
     * even though the long-polling has started working
     */
    private fun setRetrying(retryNow: CompletableDeferred<Unit>) {
        text.set("Syncing...")
        hideButton.set(true)
        hideStatus.set(false)
        if (!retryNow.isCompleted) retryNow.complete(Unit)
    }

    // various states
    sealed class Variants {
        class Normal(): Variants()
        class FullSync(): Variants()
        // network issue that may be temporary
        class NeedRetry(val err: Exception, val retryNow: CompletableDeferred<Unit>): Variants()
        // authentication error
        class NeedRelogin(val err: Exception, val restart: CompletableDeferred<Unit>): Variants()
    }
}
