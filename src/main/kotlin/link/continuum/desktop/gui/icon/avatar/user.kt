package link.continuum.desktop.gui.icon.avatar

import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.http.MediaServer
import link.continuum.desktop.util.onNone
import link.continuum.desktop.util.onSome
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
private val counter = AtomicInteger(0)

/**
 * the server through which we got this user
 * also where we should download the avatar from
 */
typealias SelectUser = Pair<UserId, HttpUrl>

@ExperimentalCoroutinesApi
class AvatarView(
        private val userData: UserDataStore,
        avatarSize: Double
) {
    val root = StackPane()
    private val initialIcon = InitialIcon(avatarSize)
    private val imageView = ImageView()

    private val user = Channel<Pair<UserId, Server>>(Channel.CONFLATED)

    init {
        logger.debug { "creating AvatarView ${counter.getAndIncrement()}" }
        root.minHeight = avatarSize
        root.minWidth = avatarSize
        root.add(initialIcon.root)
        root.add(imageView)

        GlobalScope.launch {
            switchUpdateUser(user)
        }
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
