package link.continuum.desktop.gui.list.user

import javafx.scene.image.Image
import javafx.scene.paint.Color
import koma.gui.element.icon.avatar.processing.processAvatar
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.models.getLatestAvatar
import link.continuum.desktop.database.models.getLatestNick
import link.continuum.desktop.database.models.saveUserAvatar
import link.continuum.desktop.database.models.saveUserNick
import link.continuum.desktop.gui.UpdateConflater
import link.continuum.desktop.gui.icon.avatar.DeferredImage
import link.continuum.desktop.gui.switchGetDeferred
import link.continuum.desktop.util.Option
import mu.KotlinLogging
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class UserDataStore(private val data: KDataStore) {
    private val nameUpdates = ConcurrentHashMap<UserId, UpdateConflater<String>>()
    suspend fun updateName(userId: UserId, name: String, time: Long) {
        val c = nameUpdates.get(userId)
        if (c!= null) {
            logger.debug { "sending user name update, $userId, $name" }
            c.update(time, name)
        }
        saveUserNick(data, userId, name, time)
    }
    private val color = ConcurrentHashMap<UserId, Color>()
    fun getUserColor(userId: UserId): Color {
        return color.computeIfAbsent(userId) { hashStringColorDark(it.str) }
    }


    fun getNameUpdates(userId: UserId): ReceiveChannel<String> {
        val u = nameUpdates.computeIfAbsent(userId) { UpdateConflater() }
        GlobalScope.launch {
            val n = getLatestNick(data, userId)
            if (n != null) {
                u.update(n.since, n.nickname)
            } else {
                logger.debug { "name of $userId is not in database" }
            }
        }
        val c = u.subscribe()
        return c
    }
    val avatarFetcher = DeferredImage({ i -> processAvatar(i)})
    private val avatarUrlUpdates = ConcurrentHashMap<UserId, UpdateConflater<String>>()
    private val avatarImageUpdates = ConcurrentHashMap<UserId, ConflatedBroadcastChannel<Option<Image>>>()
    suspend fun updateAvatarUrl(userId: UserId, avatarUrl: String, time: Long) {
        val c = avatarUrlUpdates.get(userId)
        if (c!= null) {
            logger.debug { "sending user avatarUrl update, $userId, $avatarUrl" }
            c.update(time, avatarUrl)
        }
        saveUserAvatar(data, userId, avatarUrl, time)
    }
    fun getAvatarImageUpdates(userId: UserId, client: OkHttpClient): ReceiveChannel<Option<Image>> {
        val imageBroadcast = avatarImageUpdates.computeIfAbsent(userId) {
            val urls = getAvatarUrlUpdates(userId)
            val av = ConflatedBroadcastChannel<Option<Image>>()
            GlobalScope.switchGetDeferred(urls, { u -> avatarFetcher.getDeferred(u, client) }, av)
            av
        }
        return imageBroadcast.openSubscription()
    }

    fun getAvatarUrlUpdates(userId: UserId): ReceiveChannel<String> {
        val u = avatarUrlUpdates.computeIfAbsent(userId) {
            val up = UpdateConflater<String>()
            GlobalScope.launch {
                val n = getLatestAvatar(data, userId)
                if (n != null) {
                    up.update(n.since, n.avatar)
                } else {
                    logger.trace { "avatarUrl of $userId is not in database" }
                }
            }
            up
        }
        val c = u.subscribe()
        return c
    }
}
