package link.continuum.desktop.gui.list.user

import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import link.continuum.database.KDataStore
import link.continuum.database.models.getLatestAvatar
import link.continuum.database.models.getLatestNick
import link.continuum.database.models.saveUserAvatar
import link.continuum.database.models.saveUserNick
import link.continuum.desktop.database.LatestFlowMap
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.util.UiPool
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class UserDataStore(
        val data: KDataStore
) {
    val avatarPool = UiPool { AvatarView(this) }
    val latestNames = LatestFlowMap(
            save = { userId: UserId, s: String?, l: Long ->
                s?.let { saveUserNick(data, userId, it, l) }
            },
            init = {
                getLatestNick(data, it)?.let { it.since to it.nickname } ?: 0L to null
            })
    suspend fun updateName(userId: UserId, name: String, time: Long) {
        latestNames.update(userId, name, time)
    }
    private val color = ConcurrentHashMap<UserId, Color>()
    fun getUserColor(userId: UserId): Color {
        return color.computeIfAbsent(userId) { hashStringColorDark(it.str) }
    }
    fun getNameUpdates(userId: UserId): Flow<String?> {
        return latestNames.receiveUpdates(userId)
    }

    val latestAvatarUrls = LatestFlowMap(
            save = { userId: UserId, url: MHUrl?, l: Long ->
                url?.let { saveUserAvatar(data, userId, it.toString(), l) }
            },
            init = {
                val n = getLatestAvatar(data, it)
                val a = n?.avatar?.parseMxc()
                if (n != null && a != null) {
                    n.since to a
                } else 0L to null
            })
    suspend fun updateAvatarUrl(userId: UserId, avatarUrl: MHUrl, time: Long) {
        latestAvatarUrls.update(userId, avatarUrl, time)
    }
    fun getAvatarUrlUpdates(userId: UserId): Flow<MHUrl?> {
        return latestAvatarUrls.receiveUpdates(userId)
    }
}
