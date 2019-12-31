package link.continuum.desktop.gui.notification

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.media.AudioClip
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.koma_app.AppStore
import koma.matrix.NotificationResponse
import koma.matrix.event.room_message.RoomEventType
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.StyleBuilder
import link.continuum.desktop.gui.em
import link.continuum.desktop.gui.util.Recyclable
import link.continuum.desktop.util.debugAssertUiThread
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging
import org.controlsfx.control.Notifications

private typealias NotificationData = NotificationResponse.Notification
private val logger = KotlinLogging.logger {}

fun popNotify(notification: NotificationData, store: AppStore, server: MediaServer) {
    debugAssertUiThread()
    val graphic = NotificationGraphic(store)
    val popup = Notifications.create()
            .onAction {
                graphic.close()
            }
            .title("Incoming message")
            .position(Pos.TOP_RIGHT)
    graphic.updateItem(notification, server)
    popup.graphic(graphic.graphic)
    popup.show()
}

/**
 * AudioClip can be played on any thread
 * But there seems to be no sound when played without starting JFX
 */
object AudioResources {
    val message by lazy {
        AudioClip(javaClass.getResource("/sound/message.m4a").toExternalForm())
    }
}

private class NotificationGraphic(
        store: AppStore
) {
    var graphic: Node? = null
        get() = field
        private set(value) {field = value}
    val root = object : StackPane() {
        init {
            style = boxStyle
        }

        override fun computeMinHeight(width: Double): Double {
            return super.computeMinHeight(width) + 60.0
        }
    }
    private val messageView = Recyclable(store.messageCells)
    private val fallbackCell = TextFlow()

    fun updateItem(item: NotificationData?, server: MediaServer) {
        messageView.recycle()
        if (null == item) {
            graphic = null
            return
        }
        val event = item.event
        val content = event.content
        root.children.clear()
        when {
            event.type == RoomEventType.Message -> if(content is NotificationResponse.Content.Message) {
                val msgView = messageView.get()
                val msg = content.message
                msgView.update(event, server, msg)
                root.children.add(msgView.root)
            } else {
                logger.debug { "msg content $content"}
            }
            else -> {
                fallbackCell.children.setAll(Text("type: ${event.type}, content: ${event.content}"))
                root.children.add(fallbackCell)
            }
        }
        graphic = root
    }
    init {
    }

    companion object {
        private val boxStyle = StyleBuilder {
            prefWidth = 12.em
        }.toStyle()
    }

    fun close() {
        messageView.recycle()
    }
}