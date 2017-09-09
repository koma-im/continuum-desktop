package view

import controller.guiEvents
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.view.ChatMainView
import koma.gui.view.listview.RoomListView
import koma.model.user.UserItemModel
import koma.model.user.UserState
import koma.storage.rooms.RoomStore
import koma_app.appState
import model.*
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.schedulers.Schedulers
import service.getMedia
import tornadofx.*
import java.io.ByteArrayInputStream


/**
 * Created by developer on 2017/6/21.
 */

class WidthModel(val width: ReadOnlyDoubleProperty) : ViewModel()

class ChatView(): View() {

    override val root = vbox (spacing = 5.0)

    val roomListView: RoomListView by inject()
    val chatMainView: ChatMainView by inject()

    lateinit var spane: SplitPane
    var selected_room_once = false

    init {
        with(root) {

            spane = splitpane() {
                vgrow = Priority.ALWAYS

                vbox(spacing = 10.0) {
                    SplitPane.setResizableWithParent(this@vbox, false)
                    add(roomListView)
                }

                //+chatMainView
                add(chatMainView)

                //+userListView
                listview(appState.currUserList) {
                    SplitPane.setResizableWithParent(this@listview, false)
                    cellFragment(UserFragment::class)
                }
            }

            label("standing by") {
                guiEvents.statusMessage.toObservable().observeOnFx().subscribe { text = it }
            }
        }

        RoomStore.roomList.addListener { observable, oldValue, newValue ->
            if ( !selected_room_once && newValue.isNotEmpty()) {
                roomListView.root.selectionModel.selectFirst()
                selected_room_once = true
            }
        }
    }

}


class UserFragment : ListCellFragment<UserState>() {

    val us = UserItemModel(itemProperty)

    override val root = hbox(spacing = 10.0) {
        style {
            alignment = Pos.CENTER_LEFT
        }
        stackpane {
            val iv = ImageView()
            iv.imageProperty().bind(us.avatar)
            iv.isCache = true
            iv.isPreserveRatio = true
            this += iv

            minWidth = 32.0
            alignment = Pos.CENTER
        }

        vbox(spacing = 2.0) {
            text(us.name) {
                this.fillProperty().bind(us.color)
            }
            //text(us.typing) {
            //}
        }

        alignment = Pos.CENTER_LEFT
    }
}

class MessageFragment: ListCellFragment<MessageItem>() {

    val msg = MessageItemModel(itemProperty)
    val widthmodel: WidthModel by inject()
    val sender = msg.sender.select { userState -> userState.displayName }
    val avtar = msg.sender.select { us -> us.avatarImgProperty }
    val color = msg.sender.select { us -> us.colorProperty }

    override val root = hbox(spacing = 10.0) {
        style {
            alignment = Pos.CENTER_LEFT
        }
        vbox {
            imageview(avtar) {
                isCache = true
                isPreserveRatio = true
            }
            text(msg.date)
        }

        minWidth = 32.0

        vbox(spacing = 2.0) {
            text(sender) {
                fillProperty().bind(color)
            }
            hbox(spacing = 5.0) {
                val value = msg.message
                this += renderMessage(value, widthmodel.width.subtract(130))
            }
        }
    }
}

fun renderMessage(msg: Property<MsgType>, width: DoubleBinding): Node {
    val group = Group()
    msg.toObservable().observeOn(Schedulers.io()).map {
        when (it) {
            is TextMsg -> {
                val tf = TextFlow()
                tf.maxWidthProperty().bind(width)
                val t = Text(it.text)
                tf.add(t)
                tf
            }
            is ImageMsg -> {
                val media = getMedia(it.mxcurl)
                if (media != null) {
                    val im = Image(ByteArrayInputStream(media), 100.0, 320.0, true, false)
                    group.tooltip(it.desc)
                    ImageView(im)
                } else {
                    Text("broken image")
                }
            }
            else -> Text("unexpected")
        }
    }.observeOnFx()
            .subscribe { group.children.setAll(listOf(it))}
    return group
}

class RoomFragment: ListCellFragment<Room>() {

    val room = RoomItemModel(itemProperty)

    override val root = hbox(spacing = 10.0) {
        style {
            alignment = Pos.CENTER_LEFT
        }
        val iv = ImageView()
        iv.imageProperty().bind(room.icon)
        iv.isCache = true
        iv.isPreserveRatio = true
        this += iv

        minWidth = 32.0
        alignment = Pos.CENTER

        text(room.name) {
            fillProperty().bind(room.color)
        }
    }
}


