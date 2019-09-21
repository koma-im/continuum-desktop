package koma.gui.view.window.chatroom.messaging.reading.display.room_event.member

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.text.Text
import koma.Koma
import koma.Server
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.StateEventUserView
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.room.participation.Membership
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.component.FitImageRegion
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.util.http.DL
import link.continuum.desktop.util.http.MediaServer
import link.continuum.desktop.util.http.urlChannelDownload
import link.continuum.desktop.util.onNone
import link.continuum.desktop.util.onSome
import link.continuum.desktop.util.toOption
import model.Room
import mu.KotlinLogging
import okhttp3.OkHttpClient
import java.util.*

private val AppSettings = appState.store.settings
private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MRoomMemberViewNode(
        store: AppStore
): MessageCell(store) {
    override val center = StackPane()
    init {
        node.add(center)
    }
    private val scaling: Float = store.settings.scaling
    private val avatarsize: Double = scaling * 32.0
    private val minWid: Double = scaling * 40.0

    private val userView = StateEventUserView(store.userData)
    private val timeView = DatatimeView()
    private val contentPane = StackPane()

    private val inviterView = StateEventUserView(store.userData)
    private val invitationContent by lazy {
        HBox(5.0).apply {
            alignment = Pos.CENTER
        }
    }
    private val joinedContent by lazy {
        HBox(5.0).apply {
            alignment = Pos.CENTER
        }
    }
    private val userUpdate = UserAppearanceUpdateView(avatarsize = avatarsize, minWid = minWid)

    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            val ev = item.first.getEvent()
            if (ev !is MRoomMember) {
                graphic = null
            } else {
                updateEvent(item.first, item.second)
                update(ev, item.second.account.server)
                graphic = node
            }
        }
    }
    private fun update(message: MRoomMember, server: MediaServer) {
        userView.updateUser(message.sender, server)
        timeView.updateTime(message.origin_server_ts)
        contentPane.children.clear()
        when (message.content.membership) {
            Membership.join -> updateJoin(message, server)
            Membership.invite -> updateInvite(message, server)
        }
    }
    private fun updateInvite(message: MRoomMember, server: MediaServer) {
        val invitee = message.state_key ?: return
        inviterView.updateUser(UserId(invitee), server)
        invitationContent.children.clear()
        invitationContent.children.addAll(Text("invited"), inviterView.root)
        contentPane.children.addAll(invitationContent)
    }
    private fun updateJoin(message: MRoomMember, server: Server) {
        val pc = message.prev_content ?: message.unsigned?.prev_content
        if (pc != null && pc.membership == Membership.join) {
            userUpdate.updateName(pc.displayname, message.content.displayname)

            userUpdate.updateAvatar(
                    pc.avatar_url?.parseMxc(),
                    message.content.avatar_url?.parseMxc(), server)
            contentPane.children.addAll(userUpdate.root)
        } else {
            joinedContent.children.clear()
            if (pc != null && pc.membership == Membership.invite) {
                joinedContent.children.addAll(Text("accepted invitation"))
                val invi = message.content.inviter
                if (invi != null) {
                    inviterView.updateUser(invi, server)
                    joinedContent.children.addAll(Text("from"), inviterView.root)
                }
                joinedContent.children.addAll(Text("and"))
            }
            joinedContent.children.addAll(Text("joined"))
            contentPane.children.addAll(joinedContent)
        }
    }
    init {
        node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                add(userView.root)
                add(contentPane)
                add(timeView.root)
            }
        }
    }
}

@ExperimentalCoroutinesApi
private class InvitationContent(
        client: OkHttpClient,
        store: UserDataStore
) {
    val userView = StateEventUserView(store )
    val root =  HBox(5.0).apply {
        alignment = Pos.CENTER
        text("invited")
        add(userView.root)
    }
}

@ExperimentalCoroutinesApi
class UserAppearanceUpdateView(
        private val avatarsize: Double,
        private val minWid: Double
){
    val root = VBox()
    private val avatarChangeView: HBox
    private val oldAvatar = ImageViewAsync().apply {
        root.style = avStyle
    }
    private val newAvatar = ImageViewAsync().apply {
        root.style = avStyle
    }
    private val nameChangeView: HBox
    private val oldName = Text()
    private val newName = Text()

    fun updateAvatar(old: MHUrl?, new: MHUrl?, mediaServer: Server) {
        avatarChangeView.showIf(old != new)
        oldAvatar.updateUrl(old.toOption(), mediaServer)
        newAvatar.updateUrl(new.toOption(),mediaServer)
    }
    fun updateName(old: String?, new: String?) {
        nameChangeView.showIf(old != new)
        oldName.text = old
        newName.text = new
    }
    init {

        with(root) {
            alignment = Pos.CENTER
            avatarChangeView = hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("updated avatar") {
                    opacity = 0.5
                }
                add(oldAvatar.root)
                addArrowIcon()
                add(newAvatar.root)
            }

            nameChangeView = hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("updated name") {
                    opacity = 0.5
                }
                stackpane {
                    minWidth = 50.0
                    add(oldName)
                }
                addArrowIcon()
                stackpane {
                    add(newName)
                    minWidth = 50.0
                }
            }
        }
    }

    companion object {
        private val avStyle: String = StyleBuilder().apply {
            val size = 2.em()
            minHeight = size
            minWidth = size
            maxHeight = size
            maxWidth = size
            prefHeight = size
            prefWidth = size
        }.toString()
    }
}

class ImageViewAsync: CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root = FitImageRegion()
    private val urlChannel: SendChannel<DL>
    fun updateUrl(url: Optional<MHUrl>, server: Server) {
        logger.trace { "ImageViewAsync update url $url" }
        if (!urlChannel.offer(url.map { it to server })) {
            logger.error { "url $url not offered successfully" }
        }
    }

    init {
        val (tx, rx) = urlChannelDownload()
        urlChannel = tx
        launch {
            for (i in rx) {
                i.onSome {
                    it.inputStream().use {
                        val im = processAvatar(it)
                        withContext(Dispatchers.JavaFx) {
                            root.image = im
                        }
                    }
                }.onNone {
                    root.image = null
                }
            }
        }
    }
}

private fun Pane.addArrowIcon() {
    val arrowico = FontAwesomeIconFactory.get().createIcon(
            FontAwesomeIcon.ARROW_RIGHT,
            AppSettings.scale_em(1f))
    arrowico.opacity = 0.3
    this.add(arrowico)
}
