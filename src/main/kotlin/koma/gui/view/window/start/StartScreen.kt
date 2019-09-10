package koma.gui.view.window.start

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import koma.gui.view.LoginScreen
import koma.koma_app.AppStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.controlsfx.control.MaskerPane
import kotlin.time.ClockMark
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class StartScreen(
        private val startTime: ClockMark
) {

    val root = StackPane()
    private val login = CompletableDeferred<LoginScreen>()
    fun initialize() {
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
        val l = LoginScreen(mask)
        innerBox.children.add(l.root)
        login.complete(l)
    }

    suspend fun start(appStore: AppStore) {
        login.await().start(appStore)
    }
}
