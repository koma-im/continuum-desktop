package link.continuum.desktop.gui.icon.avatar

import javafx.scene.layout.Region
import javafx.scene.paint.Color
import koma.Server
import koma.matrix.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.observable.set
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class AvatarView(
        private val userData: UserDataStore
): CoroutineScope by MainScope() {
    private val avatar = Avatar2L()
    val root: Region
        get() = avatar.root

    private val scope = MainScope()
    private lateinit var server: MediaServer
    private val user = MutableStateFlow<UserId?>(null)

    init {
        user.dropWhile {
            it == null
        }.onEach {
            if (it == null) {
                avatar.initialIcon.updateColor(Color.GRAY)
                avatar.initialIcon.updateString("")
            } else {
                val color = userData.getUserColor(it)
                avatar.initialIcon.updateColor(color)
            }
        }.filterNotNull().flatMapLatest {
            userData.getNameUpdates(it)
        }.onEach {
            avatar.initialIcon.updateString(it ?: "")
        }.launchIn(scope)

        user.dropWhile {
            it == null
        }.onEach {
            if (it == null) {
                avatar.imageView.setMxc(null, server)
            }
        }.filterNotNull().flatMapLatest {
            userData.getAvatarUrlUpdates(it)
        }.onEach {
            avatar.updateUrl(it, server)
        }.launchIn(scope)
    }

    fun updateUser(userId: UserId, server: MediaServer) {
        this.server = server
        user.set(userId)
    }
}
