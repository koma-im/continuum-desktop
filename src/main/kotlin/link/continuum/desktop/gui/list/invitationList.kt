package link.continuum.desktop.gui.list

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.Server
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import link.continuum.desktop.events.InviteData
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.Avatar2L
import link.continuum.desktop.gui.icon.avatar.AvatarInline
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class InvitationsView(
        private val scaling: Double = 1.0
) {
    val list = VBox(3.0)
    @Deprecated("find the right way to avoid dupliicates when a full sync is performed")
    private val added = mutableSetOf<RoomId>()
    private val spareCells = mutableListOf<InvitationCell>()

    init {
        list.isFocusTraversable = false
    }

    fun add(invite: InviteData, server: Server) {
        debugAssertUiThread()
        if (added.contains(invite.id)) {
            logger.warn { "ignoring duplicate invite $invite" }
            return
        } else {
            added.add(invite.id)
        }
        val c = if (spareCells.isNotEmpty()) {
            spareCells.removeAt(0)
        } else{
            InvitationCell(scaling)
        }
        c.update(invite,
                server = server)
        list.add(c.cell)
    }

    private inner class InvitationCell(
            private val scaling: Double = 1.0
    ) {
        private val inviter = Label()
        private val inviterAvatar = AvatarInline()
        private val roomAvatar = Avatar2L()
        private val roomLabel = Text()
        private var roomId: RoomId? = null


        val cell = HBox(3.0*scaling).apply {
            minWidth = 1.0
            prefWidth = 1.0
            add(roomAvatar.root)
            vbox {
                add(TextFlow().apply {
                    add(inviterAvatar.root)
                    text(" ")
                    add(inviter)
                    text(" invited you to room ")
                    add(roomLabel)
                })
                hbox(2.0*scaling) {
                    alignment = Pos.CENTER_RIGHT
                    HBox.setHgrow(this, Priority.ALWAYS)

                    button("Join").action {
                        logger.debug { "Accepting invitation to $roomId" }
                        roomId?.let {
                            logger.debug { "joining $it" }
                            GlobalScope.launch {
                                val j = appState.apiClient?.joinRoom(it)?.getOrNull()?: return@launch run {
                                    logger.warn { "failed to join $roomId, ${it}" }
                                }
                                logger.debug { "joined room $j" }
                            }
                        }
                        remove()
                    }
                    button("Ignore").action {
                        logger.debug { "Ignoring invitation to $roomId" }
                        remove()
                    }
                }
            }
        }

        fun update(
                invitation: InviteData,
                server: Server
        ) {
            debugAssertUiThread()
            roomId = invitation.id
            inviter.text = invitation.inviterName

            inviterAvatar.updateName(
                    invitation.inviterName?:"   ",
                    hashStringColorDark(invitation.inviterId?.str?:""))
            inviterAvatar.updateUrl(invitation.inviterAvatar?.parseMxc(), server)

            roomLabel.text = invitation.roomDisplayName

            roomAvatar.updateName(invitation.roomDisplayName?:"   ",
                    hashStringColorDark(invitation.id.str))
            roomAvatar.updateUrl(invitation.roomAvatar?.parseMxc(), server)
        }

        fun remove() {
            debugAssertUiThread()
            list.children.remove(this.cell)
            spareCells.add(this)
        }
    }
}
