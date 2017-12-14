package view

import controller.ChatController
import controller.guiEvents
import koma.controller.requests.membership.ask_invite_member
import koma.controller.requests.membership.ask_join_room
import koma.gui.view.window.preferences.PreferenceWindow
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import tornadofx.*

/**
 * Created by developer on 2017/6/17.
 */
class RootLayoutView(val controller: ChatController): View() {
    public override val root = borderpane()

    init {
        with(root) {
            top = menubar {
                menu("File") {
                    item("Create Room").actionEvents().addTo(
                            guiEvents.createRoomRequests)
                    item("Join Room") {
                        action { ask_join_room() }
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
                    item("Ban Member").actionEvents().addTo(
                            guiEvents.banMemberRequests)
                }
                menu("Me") {
                    item("Upload media").actionEvents().addTo(
                            guiEvents.updateAvatar)
                    item("Update my name").action { controller.updateMyAlias() }
                }
            }
        }
    }
}
