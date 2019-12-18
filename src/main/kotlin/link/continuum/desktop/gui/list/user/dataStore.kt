package link.continuum.desktop.gui.list.user

import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import link.continuum.database.KDataStore
import link.continuum.database.models.getLatestAvatar
import link.continuum.database.models.getLatestNick
import link.continuum.database.models.saveUserAvatar
import link.continuum.database.models.saveUserNick
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.util.UiPool
import link.continuum.desktop.observable.MutableObservable
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class UserDataStore(
        val data: KDataStore
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val scope = CoroutineScope(Dispatchers.Default)
    val avatarPool = UiPool { AvatarView(this) }
    private val nameUpdates = ConcurrentHashMap<UserId, MutableObservable<Pair<Long, String?>>>()
    suspend fun updateName(userId: UserId, name: String, time: Long) {
        val c = nameUpdates.get(userId)
        if (c!= null) {
            logger.debug { "sending user name update, $userId, $name" }
            val current = c.get()
            if (time >= current.first) {
                c.set(time to name)
            }
        }
        saveUserNick(data, userId, name, time)
    }
    private val color = ConcurrentHashMap<UserId, Color>()
    fun getUserColor(userId: UserId): Color {
        return color.computeIfAbsent(userId) { hashStringColorDark(it.str) }
    }


    fun getNameUpdates(userId: UserId): Flow<String?> {
        val u = nameUpdates.computeIfAbsent(userId) {
            MutableObservable<Pair<Long, String?>>(0L to null).also {
                launch {
                    val n = getLatestNick(data, userId)
                    if (n != null) {
                        it.set(n.since to n.nickname)
                    } else {
                        logger.debug { "name of $userId is not in database" }
                    }
                }
            }
        }
        val c = u.flow().map {
            it.second
        }
        return c.distinctUntilChanged()
    }
    private val avatarUrlUpdates = ConcurrentHashMap<UserId, MutableObservable<Pair<Long, MHUrl?>>>()
    suspend fun updateAvatarUrl(userId: UserId, avatarUrl: MHUrl, time: Long) {
        val c = avatarUrlUpdates.get(userId)
        if (c!= null && c.get().first <= time) {
            logger.debug { "sending user avatarUrl update, $userId, $avatarUrl" }
            c.set(time to avatarUrl)
        }
        saveUserAvatar(data, userId, avatarUrl.toString(), time)
    }

    fun getAvatarUrlUpdates(userId: UserId): Flow<MHUrl?> {
        val u = avatarUrlUpdates.computeIfAbsent(userId) {
            val up = MutableObservable<Pair<Long, MHUrl?>>(0L to null)
            launch {
                val n = getLatestAvatar(data, userId)
                if (n != null) {
                    val avatar = n.avatar.parseMxc()
                    avatar?.let {
                        up.set(n.since to it)
                    } ?: logger.warn { "avatarUrl of $userId, ${n.avatar} not valid" }
                } else {
                    logger.trace { "avatarUrl of $userId is not in database" }
                }
            }
            up
        }
        val c = u.flow().map {it.second}
        return c.distinctUntilChanged()
    }
}
