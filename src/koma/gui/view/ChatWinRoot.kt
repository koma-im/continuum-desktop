package view

import controller.ChatController
import controller.guiEvents
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
                    item("Join Room").actionEvents().addTo(
                            guiEvents.joinRoomRequests)
                    item("Quit").action {
                        FX.primaryStage.close()
                    }
                }
                menu("Room") {
                    item("Invite Member").actionEvents().addTo(
                            guiEvents.inviteMemberRequests)
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
