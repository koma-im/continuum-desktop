package koma.gui.view.window.start

import javafx.geometry.Pos
import koma.gui.view.LoginScreen
import koma.koma_app.AppData
import koma.koma_app.AppStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.controlsfx.control.MaskerPane
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class StartScreen(
        private val startTime: TimeMark
) {

    val root = StackPane()
    val login = CompletableDeferred<LoginScreen>()
    fun initialize(keyValueStore: KeyValueStore, deferredAppData: Deferred<AppData>) {
        debugAssertUiThread()
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
        val l = LoginScreen(keyValueStore, deferredAppData, mask)
        innerBox.children.add(l.root)
        login.complete(l)
    }
}
