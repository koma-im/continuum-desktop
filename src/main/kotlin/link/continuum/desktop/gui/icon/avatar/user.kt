package link.continuum.desktop.gui.icon.avatar

import link.continuum.desktop.gui.StackPane
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import link.continuum.desktop.gui.StyleBuilder
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.component.FitImageRegion
import link.continuum.desktop.gui.em
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.http.MediaServer
import link.continuum.desktop.util.onNone
import link.continuum.desktop.util.onSome
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
private val counter = AtomicInteger(0)

@ExperimentalCoroutinesApi
class AvatarView(
        private val userData: UserDataStore
) {
    val root = StackPane()
    private val initialIcon = InitialIcon()
    private val imageView = FitImageRegion()

    private val user = Channel<Pair<UserId, Server>>(Channel.CONFLATED)

    init {
        logger.debug { "creating AvatarView ${counter.getAndIncrement()}" }
        root.style = style
        root.add(initialIcon.root)
        root.add(imageView)

        GlobalScope.launch {
            switchUpdateUser(user)
        }
    }

    companion object {
        private val style = StyleBuilder().apply {
            val size = 2.em
            minHeight = size
            minWidth = size
            prefHeight = size
            prefWidth = size
            maxHeight = size
            maxWidth = size
        }.toStyle()
    }
    fun updateUser(userId: UserId, server: MediaServer) {
        if (!user.offer(userId to server)) {
            logger.error { "failed to update user of avatar view to $userId" }
        }
    }

    @ExperimentalCoroutinesApi
    fun CoroutineScope.switchUpdateUser(input: ReceiveChannel<Pair<UserId, Server>>) {
        launch {
            var current = input.receive()
            var name = userData.getNameUpdates(current.first)
            var image = userData.getAvatarImageUpdates(current.first, current.second)
            loop@ while (isActive) {
                val noMore = select<Boolean> {
                    input.onReceiveOrNull { k ->
                        k?.let {
                            logger.trace { "switching to avatar for $k" }
                            current = it
                            name.cancel()
                            image.cancel()
                            initialIcon.show()
                            imageView.image = null
                            name = userData.getNameUpdates(current.first)
                            image = userData.getAvatarImageUpdates(current.first, current.second)
                            false
                        } ?: true
                    }
                    image.onReceive {
                        logger.trace { "got updated ${it.isPresent} image for $current" }
                        withContext(Dispatchers.JavaFx) {
                            it.onSome {
                                imageView.image = it
                                initialIcon.hide()
                            }.onNone {
                                initialIcon.show()
                                imageView.image = null
                            }
                        }
                        false
                    }
                    name.onReceive {
                        logger.trace { "getting generated avatar for $current with name $it" }
                        withContext(Dispatchers.JavaFx) {
                            val color = userData.getUserColor(current.first)
                            initialIcon.updateItem(it, color)
                        }
                        false
                    }
                }
                if (noMore) break@loop
            }
            logger.debug { "canceling subscription" }
            input.cancel()
        }
    }
}
