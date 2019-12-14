package link.continuum.desktop.gui.icon.avatar

import javafx.scene.layout.Region
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
private val counter = AtomicInteger(0)

class AvatarView(
        private val userData: UserDataStore
): CoroutineScope by MainScope() {
    private val avatar = Avatar2L()
    val root: Region
        get() = avatar.root

    private val scope = MainScope()
    private val user = MutableObservable<Pair<UserId, Server>>()

    init {
        logger.debug { "creating AvatarView ${counter.getAndIncrement()}" }

        user.flow().flatMapLatest {
            userData.getNameUpdates(it.first)
        }.onEach {
            val color = userData.getUserColor(user.get().first)
            if ( it != null) avatar.updateName(it, color)
        }.launchIn(scope)
        user.flow().flatMapLatest {
            userData.getAvatarUrlUpdates(it.first)
        }.onEach {
            avatar.updateUrl(it, user.get().second)
        }.launchIn(scope)
    }

    fun updateUser(userId: UserId, server: MediaServer) {
        user.set(userId to server)
    }
}
