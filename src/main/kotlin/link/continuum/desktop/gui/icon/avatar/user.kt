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

class AvatarView(
        private val userData: UserDataStore
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val avatar = UrlAvatar()
    val root: StackPane
        get() = avatar.root

    private val user = Channel<Pair<UserId, Server>>(Channel.CONFLATED)

    init {
        logger.debug { "creating AvatarView ${counter.getAndIncrement()}" }
        root.style = style

        launch {
            switchUpdateUser(user)
        }
    }

    companion object {
        private val style = StyleBuilder().apply {
            val size = 2.em
            fixWidth(size)
            fixHeight(size)
        }.toStyle()
    }
    fun updateUser(userId: UserId, server: MediaServer) {
        if (!user.offer(userId to server)) {
            logger.error { "failed to update user of avatar view to $userId" }
        }
    }

    fun CoroutineScope.switchUpdateUser(input: ReceiveChannel<Pair<UserId, Server>>) {
        launch {
            var current = input.receive()
            var name = userData.getNameUpdates(current.first)
            var image = userData.getAvatarUrlUpdates(current.first)
            loop@ while (isActive) {
                val noMore = select<Boolean> {
                    input.onReceiveOrNull { k ->
                        k?: return@onReceiveOrNull true
                        logger.trace { "switching to avatar for $k" }
                        current = k
                        name.cancel()
                        image.cancel()
                        name = userData.getNameUpdates(current.first)
                        image = userData.getAvatarUrlUpdates(current.first)
                        false
                    }
                    image.onReceive {
                        avatar.updateUrl(it, current.second)
                        false
                    }
                    name.onReceive {
                        logger.trace { "getting generated avatar for $current with name $it" }
                        val color = userData.getUserColor(current.first)
                        withContext(Dispatchers.JavaFx) {
                            avatar.updateName(it, color)
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
