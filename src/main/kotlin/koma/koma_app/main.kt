package koma.koma_app

import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.control.Alert
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.Koma
import koma.gui.save_win_geometry
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import link.continuum.desktop.util.disk.path.getConfigDir
import link.continuum.desktop.util.disk.path.loadOptionalCert
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.logging.Level
import java.util.logging.Logger

fun main(args: Array<String>) {
    val lvl = System.getenv()["LOG_LEVEL"]?.also {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", it)
    }
    val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    if (lvl != null) {
        logger.info("log level set to {}", lvl)
    }
    Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

    Application.launch(KomaApp::class.java, *args)
    appState.coroutineScope.cancel()
}


@ExperimentalCoroutinesApi
class KomaApp : App(StartScreen::class) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    init {
        Thread.setDefaultUncaughtExceptionHandler(NoAlertErrorHandler())
    }

    private fun load() {
        val args = parameters.raw
        val arg = args.firstOrNull() ?: run {
            System.getenv()["CONTINUUM_DIR"]
        }
        val data_dir = arg ?: getConfigDir()
        log.info("data dir set to {}", data_dir)
        val (settings, db) = loadSettings(data_dir)
        val proxy = settings.proxyList.default()
        val k= Koma(proxy.toJavaNet(), path = data_dir,
                addTrust = loadOptionalCert(File(data_dir)))
        appState.koma = k
        val store = AppStore(db, settings, k)
        appState.store = store

    }

  override fun start(stage: Stage) {
      try {
          load()
      } catch (e: Exception) {
          alert(Alert.AlertType.ERROR, "couldn't open configuration directory: $e")
          return
      }

      super.start(stage)
      setSaneStageSize(stage)
      stage.hide()
      stage.show()
      stage.onCloseRequest = EventHandler<WindowEvent> {
          save_win_geometry(stage)
      }
  }

}
