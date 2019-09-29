package koma.gui.view.window.start

import javafx.geometry.Pos
import javafx.scene.layout.StackPane
import koma.gui.view.LoginScreen
import koma.koma_app.AppStore
import kotlinx.coroutines.CompletableDeferred
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.VBox
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.controlsfx.control.MaskerPane
import org.h2.mvstore.MVMap
import kotlin.time.ClockMark
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class StartScreen(
        private val startTime: ClockMark
) {

    val root = StackPane()
    private val login = CompletableDeferred<LoginScreen>()
    fun initialize(keyValueMap: MVMap<String, String>) {
        val innerBox = HBox().apply {
            alignment = Pos.CENTER
        }
        root.children.add(VBox().apply {
                    alignment = Pos.CENTER
                    children.add(innerBox)
        })
        val mask = MaskerPane().apply {
            isVisible = false
        }
        root.children.add(mask)
        val l = LoginScreen( keyValueMap, mask)
        innerBox.children.add(l.root)
        login.complete(l)
    }

    suspend fun start(appStore: AppStore, httpClient: OkHttpClient) {
        login.await().start(appStore, httpClient)
    }
}
