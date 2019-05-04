package koma.gui.view.window.chatroom.messaging.reading.display.room_event.member

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.StateEventUserView
import koma.koma_app.appState
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.room.participation.Membership
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.showIf
import link.continuum.desktop.util.Option
import link.continuum.desktop.util.http.mapMxc
import link.continuum.desktop.util.http.urlChannelDownload
import link.continuum.desktop.util.toOption
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

private val AppSettings = appState.store.settings
private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MRoomMemberViewNode(
        store: UserDataStore,
        client: OkHttpClient
): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
        get() = listOf()
    private val scaling: Float = appState.store.settings.scaling
    private val avatarsize: Double = scaling * 32.0
    private val minWid: Double = scaling * 40.0

    private val userView = StateEventUserView(store, client, avatarsize)
    private val timeView = DatatimeView()
    private val contentPane = StackPane()
    private val joinedContent = Text("joined this room.")
    private val userUpdate = UserAppearanceUpdateView(client, avatarsize = avatarsize, minWid = minWid)

    fun update(message: MRoomMember, server: HttpUrl) {
        userView.updateUser(message.sender)
        timeView.updateTime(message.origin_server_ts)
        contentPane.children.clear()
        if (message.content.membership != Membership.join) {
            return
        }
        val pc = message.prev_content ?: message.unsigned?.prev_content
        if (pc == null) {
            contentPane.children.addAll(joinedContent)
        } else {
            userUpdate.updateName(pc.displayname, message.content.displayname)

            userUpdate.updateAvatar(
                    pc.avatar_url?.let { mapMxc(it, server) },
                    message.content.avatar_url?.let{ mapMxc(it, server) })
            contentPane.children.addAll(userUpdate.root)
        }
    }
    init {
        node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                add(userView.root)
                add(contentPane)
            }
            add(timeView.root)
        }
    }
}



@ExperimentalCoroutinesApi
class UserAppearanceUpdateView(
        private val client: OkHttpClient,
        private val avatarsize: Double,
        private val minWid: Double
){
    val root = VBox()
    private val avatarChangeView: HBox
    private val oldAvatar = ImageViewAsync(client)
    private val newAvatar = ImageViewAsync(client)
    private val nameChangeView: HBox
    private val oldName = Text()
    private val newName = Text()

    fun updateAvatar(old: HttpUrl?, new: HttpUrl?) {
        avatarChangeView.showIf(old != new)
        oldAvatar.updateUrl(old.toOption())
        newAvatar.updateUrl(new.toOption())
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
                stackpane {
                    add(oldAvatar.root)
                    minHeight = avatarsize
                    minWidth = minWid
                }
                addArrowIcon()
                stackpane {
                    add(oldAvatar.root)
                    minHeight = avatarsize
                    minWidth = minWid
                }
            }

            nameChangeView = hbox(spacing = 5.0) {
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
}

class ImageViewAsync(client: OkHttpClient) {
    val root = ImageView()
    private val urlChannel: SendChannel<Option<HttpUrl>>
    fun updateUrl(url: Option<HttpUrl>) {
        logger.trace { "ImageViewAsync update url $url" }
        if (!urlChannel.offer(url)) {
            logger.error { "url $url not offered successfully" }
        }
    }

    init {
        val (tx, rx) = GlobalScope.urlChannelDownload(client)
        urlChannel = tx
        GlobalScope.launch {
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

private fun EventTarget.addArrowIcon() {
    val arrowico = FontAwesomeIconFactory.get().createIcon(
            FontAwesomeIcon.ARROW_RIGHT,
            AppSettings.scale_em(1f))
    arrowico.opacity = 0.3
    this.add(arrowico)
}
