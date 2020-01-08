package link.continuum.desktop.action

import koma.Server
import koma.gui.view.ChatWindowBars
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.models.updateAccountUsage
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.JFX
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val logger = KotlinLogging.logger {}

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
@ExperimentalCoroutinesApi
suspend fun startChat(httpClient: OkHttpClient, userId: UserId, token: String, url: HttpUrl,
                      keyValueStore: KeyValueStore,
              appData: AppStore
) {
    val data = appData.database
    val app = appState
    val server = Server(url, httpClient)
    val account  = server.account(userId, token)
    app.apiClient = account

    val primary = ChatWindowBars(account, keyValueStore, app.job, appData)
    JFX.primaryPane.setChild(primary.root)
    data.letOp {
        updateAccountUsage(it, userId)
    }

}
