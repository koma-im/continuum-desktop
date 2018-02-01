package koma.gui.view.messagesview.fragment

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.media.getMxcImagePropery
import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.ImageMessage
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.room.participation.Membership
import koma.storage.users.UserStore
import org.fxmisc.flowless.Cell
import tornadofx.*
import java.text.SimpleDateFormat
import java.util.*

fun MRoomMessage.render_node(): Node {
    val node = TextFlow()
    val content = this.content
    when(content) {
        is TextMessage -> {
            val text = Text(content.body)
            node.add(text)
        }
        is EmoteMessage-> {
            node.add(Text(content.body))
        }
        is ImageMessage-> {
            val im = ImageView()
            im.tooltip(content.body)
            im.imageProperty().bind(getMxcImagePropery(content.url, 320.0, 120.0))
            node.add(im)
        }
    }
    return node
}

private fun showUser(node: Node, userId: UserId) {
    val user = UserStore.getOrCreateUserId(userId)
    node.apply {
        hbox(spacing = 5.0) {
            imageview(user.avatarImgProperty)
            vbox {
                alignment = Pos.CENTER
                label(user.displayName) {
                    minWidth = 50.0
                    maxWidth = 100.0
                    textFill = user.color
                }
            }
        }
    }
}

private fun showDatetime(node: Node, ts: Long) {
    val datetime= Date(ts)
    node.apply {
        hbox {
            hgrow = Priority.ALWAYS
            text(SimpleDateFormat("MM-dd HH:mm").format(datetime)) {
                opacity = 0.4
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}

class MessageCell(val message: RoomEvent): Cell<RoomEvent, Node> {
    private val _node = StackPane()

    init {
        _node.paddingAll = 2.0
        when(message) {
            is MRoomMember -> renderMemberChange(message)
            is MRoomCreate -> renderRoomCreation(message)
            is MRoomMessage -> renderMessageFromUser(message)
        }
    }

    private fun renderRoomCreation(message: MRoomCreate) {
        _node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("This room is create by") {
                    opacity = 0.5
                }
                showUser(this, message.sender)
            }
            showDatetime(this, message.origin_server_ts)
        }
    }

    private fun renderMemberChange(message: MRoomMember) {
        val content = message.content
        if (content.membership != Membership.join) return
        val pc = message.prev_content
        if (pc == null) {
            _node.apply {
                hbox(spacing = 5.0) {
                    alignment = Pos.CENTER
                    showUser(this, message.sender)
                    text("joined this room.")
                }
                showDatetime(this, message.origin_server_ts)
            }
        } else {
         _node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                showUser(this, message.sender)
                vbox {
                    alignment = Pos.CENTER
                    if (pc.avatar_url != content.avatar_url) {
                        hbox(spacing = 5.0) {
                            alignment = Pos.CENTER
                            text("updated avatar") {
                                opacity = 0.5
                            }
                            stackpane {
                                pc.avatar_url?.let { imageview(getMxcImagePropery(it, 32.0, 32.0)) }
                                minWidth = 40.0
                            }
                            val arrowico = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ARROW_RIGHT)
                            arrowico.opacity = 0.3
                            add(arrowico)
                            stackpane {
                                content.avatar_url?.let { imageview(getMxcImagePropery(it, 32.0, 32.0)) }
                                minWidth = 40.0
                            }
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
                            val arrowico = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ARROW_RIGHT)
                            arrowico.opacity = 0.3
                            add(arrowico)
                            stackpane{
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

    private fun renderMessageFromUser(item: MRoomMessage){
        val sus = UserStore.getOrCreateUserId(item.sender)
        val sender = sus.displayName
        val avtar = sus.avatarImgProperty
        val color = sus.color

        _node.apply {
            hbox {
                minWidth = 1.0
                prefWidth = 1.0
                style {
                    alignment = Pos.CENTER_LEFT
                    paddingAll = 2.0
                    backgroundColor = multi(Color.WHITE)
                }
                vbox {
                    imageview(avtar) {
                        isCache = true
                        isPreserveRatio = true
                    }
                }

                vbox(spacing = 2.0) {
                    lazyContextmenu {
                        item("Copy text").action {
                            item.content?.body?.let {
                                Clipboard.getSystemClipboard().putString(it)
                            }
                        }
                    }
                    hgrow = Priority.ALWAYS
                    hbox(spacing = 10.0) {
                        hgrow = Priority.ALWAYS
                        text(sender) {
                            fill = color
                        }

                        showDatetime(this, item.origin_server_ts)
                    }
                    hbox(spacing = 5.0) {
                        val n = item.render_node()
                        add(n)
                    }
                }
            }
        }
    }

    override fun getNode(): Node {
        return _node
    }
}

