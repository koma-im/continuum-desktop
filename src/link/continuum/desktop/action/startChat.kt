package link.continuum.desktop.action

import koma.Koma
import koma.gui.view.ChatWindowBars
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.database.models.loadUserRooms
import link.continuum.database.models.updateAccountUsage
import link.continuum.desktop.database.models.loadRoom
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.UiDispatcher
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
@ExperimentalCoroutinesApi
fun startChat(koma: Koma, userId: UserId, token: String, url: HttpUrl,
              appData: AppStore
) {
    val data = appData.database
    updateAccountUsage(data, userId)

    val app = appState
    val store = app.store
    val server = koma.server(url)
    val account  = server.account(userId, token)
    val apiClient  = account
    app.apiClient = apiClient
    val userRooms = store.joinedRoom.list

    val primary = ChatWindowBars(userRooms, account, store)
    JFX.primaryPane.setChild(primary.root)

    app.coroutineScope.launch {
        val rooms = loadUserRooms(data, userId)
        logger.debug { "user is in ${rooms.size} rooms according database records" }
        withContext(UiDispatcher) {
            rooms.forEach {
                loadRoom(store.database, it, account)?.let {
                    store.joinRoom(it.id, apiClient)
                }
            }
        }
        val fullSync = userRooms.isEmpty()
        if (fullSync) logger.warn {
            "Doing a full sync because there " +
                    "are no known rooms $userId has joined"
        }
        SyncControl(
                apiClient,
                userId,
                coroutineScope = app.coroutineScope,
                statusChan = primary.status.ctrl,
                full_sync = fullSync,
                appData = appData,
                view = primary.center
        )
    }
}
