package koma.gui.view.window.chatroom.messaging.reading.display.room_event.member

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showDatetime
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showUser
import koma.koma_app.appState
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.room.participation.Membership
import tornadofx.*

private val AppSettings = appState.store.settings

class MRoomMemberViewNode(message: MRoomMember): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
        get() = listOf()

    init {
        if (message.content.membership == Membership.join) renderJoin(message)
    }

    private fun renderJoin(message: MRoomMember) {
        val pc = message.prev_content
        if (pc == null) {
            node.apply {
                hbox(spacing = 5.0) {
                    alignment = Pos.CENTER
                    showUser(this, message.sender)
                    text("joined this room.")
                }
                showDatetime(this, message.origin_server_ts)
            }
        } else {
            node.apply {
                hbox(spacing = 5.0) {
                    alignment = Pos.CENTER
                    showUser(this, message.sender)
                    val content = message.content
                    vbox {
                        alignment = Pos.CENTER
                        if (pc.avatar_url != content.avatar_url) {
                            hbox(spacing = 5.0) {
                                alignment = Pos.CENTER
                                text("updated avatar") {
                                    opacity = 0.5
                                }
                                addAvatar(pc.avatar_url)
                                addArrowIcon()
                                addAvatar(content.avatar_url)
                            }
                        }
                        if (pc.displayname != content.displayname) {
                            hbox(spacing = 5.0) {
                                text("updated name") {
                                    opacity = 0.5
                                }
                                stackpane {
                                    minWidth = 50.0
                                    text(pc.displayname)
                                }
                                addArrowIcon()
                                stackpane {
                                    text(content.displayname)
                                    minWidth = 50.0
                                }
                            }
                        }
                    }
                }
                showDatetime(this, message.origin_server_ts)
            }
        }
    }
}

private fun EventTarget.addArrowIcon() {
    val arrowico = FontAwesomeIconFactory.get().createIcon(
            FontAwesomeIcon.ARROW_RIGHT,
            AppSettings.scale_em(1f))
    arrowico.opacity = 0.3
    this.add(arrowico)
}

private fun EventTarget.addAvatar(url: String?) {
    val avatarsize = AppSettings.scaling * 32.0
    val minWid = AppSettings.scaling * 40.0
    this.stackpane {
        url?.let { add(koma.gui.element.icon.avatar.AvatarView(it)) }
        minHeight = avatarsize
        minWidth = minWid
    }
}
