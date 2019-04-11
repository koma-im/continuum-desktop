package link.continuum.desktop.gui.icon.avatar

import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import koma.gui.element.icon.placeholder.generator.AvatarGeneratorCache
import koma.gui.element.icon.placeholder.generator.ColoredName
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import link.continuum.desktop.gui.list.user.UserDataStore
import mu.KotlinLogging
import okhttp3.OkHttpClient
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
private val counter = AtomicInteger(0)

@ExperimentalCoroutinesApi
class AvatarView(
        private val userData: UserDataStore,
        private val client: OkHttpClient,
        avatarSize: Double
) {
    val root = StackPane()
    private val imageView = ImageView()

    private val user = Channel<UserId>(Channel.CONFLATED)

    init {
        logger.debug { "creating AvatarView ${counter.getAndIncrement()}" }
        root.minHeight = avatarSize
        root.minWidth = avatarSize
        root.add(imageView)

        GlobalScope.launch {
            switchUpdateUser(user)
        }
    }
    fun updateUser(userId: UserId) {
        if (!user.offer(userId)) {
            logger.error { "failed to update user of avatar view to $userId" }
        }
    }

    @ExperimentalCoroutinesApi
    fun CoroutineScope.switchUpdateUser(input: ReceiveChannel<UserId>) {
        launch {
            var current = input.receive()
            var name = userData.getNameUpdates(current)
            var image = userData.getAvatarImageUpdates(current, client)
            var hasImage = false
            loop@ while (isActive) {
                val noMore = select<Boolean> {
                    input.onReceiveOrNull { k ->
                        k?.let {
                            logger.trace { "switching to avatar for $k" }
                            current = it
                            hasImage = false
                            name.cancel()
                            image.cancel()
                            name = userData.getNameUpdates(current)
                            image = userData.getAvatarImageUpdates(current, client)
                            false
                        } ?: true
                    }
                    image.onReceive {
                        logger.trace { "got updated ${it.isSome} image for $current" }
                        it.onSome {
                            hasImage = true
                            withContext(Dispatchers.JavaFx) {
                                imageView.image = it
                            }
                        }
                        false
                    }
                    name.onReceive {
                        if (!hasImage) {
                            logger.trace { "getting generated avatar for $current with name $it" }
                            withContext(Dispatchers.JavaFx) {
                                val av = AvatarGeneratorCache.generateAvatar(ColoredName(userData.getUserColor(current), it))
                                imageView.image = av
                            }
                        } else {
                            logger.trace { "$current has avatar image, ignoring updated name $it" }
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
