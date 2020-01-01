package link.continuum.desktop.action

import koma.Server
import koma.gui.view.ChatWindowBars
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import link.continuum.database.models.loadUserRooms
import link.continuum.database.models.updateAccountUsage
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.UiDispatcher
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.h2.mvstore.MVMap

private val logger = KotlinLogging.logger {}

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
@ExperimentalCoroutinesApi
suspend fun startChat(httpClient: OkHttpClient, userId: UserId, token: String, url: HttpUrl,
              keyValueMap: MVMap<String, String>,
              appData: AppStore
) {
    val data = appData.database
    val app = appState
    val server = Server(url, httpClient)
    val account  = server.account(userId, token)
    app.apiClient = account
    val userRooms = appData.joinedRoom.list

    val primary = ChatWindowBars(userRooms, account, keyValueMap, app.job, appData)
    JFX.primaryPane.setChild(primary.root)
    val rooms = data.letOp {
        loadUserRooms(it, userId)
    }
    logger.debug { "user is in ${rooms.size} rooms according database records" }
    withContext(UiDispatcher) {
        appData.joinedRoom.addAll(rooms)
    }
    data.letOp {
        updateAccountUsage(it, userId)
    }

}
