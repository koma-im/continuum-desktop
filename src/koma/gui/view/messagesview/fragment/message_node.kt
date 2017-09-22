package koma.gui.view.messagesview.fragment

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.media.getMxcImagePropery
import koma.model.user.UserState
import model.*
import org.fxmisc.flowless.Cell
import tornadofx.*
import java.text.SimpleDateFormat
import java.util.*

fun MessageFromOthers.render_node(): Node {
    val node = TextFlow()
    when(this) {
        is TextMsg -> {
            node.add(Text(this.text))
        }
        is EmoteMsg -> {
            node.add(Text(this.text))
        }
        is ImageMsg -> {
            val im = ImageView()
            im.tooltip(this.desc)
            im.imageProperty().bind(getMxcImagePropery(this.mxcurl, 320.0, 120.0))
            node.add(im)
        }
    }
    return node
}

private fun showUser(node: Node, user: UserState) {
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

private fun showDatetime(node: Node, datetime: Date) {
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

class MessageCell(val message: MessageToShow): Cell<MessageToShow, Node> {
    private val _node = StackPane()

    init {
        _node.paddingAll = 2.0
        when(message) {
            is MemberJoinMsg -> renderMemberJoin(message)
            is MemberUpdateMsg -> renderMemberChange(message)
            is RoomCreationMsg -> renderRoomCreation(message)
            is MessageFromOthers -> renderMessageFromUser(message)
        }
    }

    private fun renderRoomCreation(message: RoomCreationMsg) {
        _node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("This room is create by") {
                    opacity = 0.5
                }
                showUser(this, message.sender)
            }
            showDatetime(this, message.datetime)
        }
    }

    private fun renderMemberJoin(message: MemberJoinMsg) {
        _node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                showUser(this, message.sender)
                text("joined this room.")
            }
            showDatetime(this, message.datetime)
        }
    }

    private fun renderMemberChange(message: MemberUpdateMsg) {
        _node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                showUser(this, message.sender)
                vbox {
                    alignment = Pos.CENTER
                    val ac = message.avatar_change
                    if (ac.first != ac.second) {
                        hbox(spacing = 5.0) {
                            alignment = Pos.CENTER
                            text("updated avatar") {
                                opacity = 0.5
                            }
                            stackpane {
                                ac.first?.let { imageview(getMxcImagePropery(it, 32.0, 32.0)) }
                                minWidth = 40.0
                            }
                            val arrowico = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ARROW_RIGHT)
                            arrowico.opacity = 0.3
                            add(arrowico)
                            stackpane {
                                ac.second?.let { imageview(getMxcImagePropery(it, 32.0, 32.0)) }
                                minWidth = 40.0
                            }
                        }
                    }
                    val nc = message.name_change
                    if (nc.first != nc.second) {
                        hbox(spacing = 5.0) {
                            text("updated name") {
                                opacity = 0.5
                            }
                            stackpane {
                                minWidth = 50.0
                                text(nc.first)
                            }
                            val arrowico = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ARROW_RIGHT)
                            arrowico.opacity = 0.3
                            add(arrowico)
                            stackpane{
                                text(nc.second)
                                minWidth = 50.0
                            }
                        }
                    }
                }
            }
            showDatetime(this, message.datetime)
        }
    }

    private fun renderMessageFromUser(item: MessageFromOthers){
        val sender = item.sender.displayName
        val avtar = item.sender.avatarImgProperty
        val color = item.sender.color

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
                    hgrow = Priority.ALWAYS
                    hbox(spacing = 10.0) {
                        hgrow = Priority.ALWAYS
                        text(sender) {
                            fill = color
                        }

                        showDatetime(this, item.datetime)
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

fun create_message_cell(messageItem: MessageToShow): MessageCell {
    return MessageCell(messageItem)
}
