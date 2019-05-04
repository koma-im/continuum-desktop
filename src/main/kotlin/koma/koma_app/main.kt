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
import link.continuum.desktop.util.disk.path.getConfigDir
import link.continuum.desktop.util.disk.path.loadOptionalCert
import okhttp3.OkHttpClient
import tornadofx.*
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.javafx.JavaFx as UI

fun main(args: Array<String>) {
    Logger.getLogger(OkHttpClient::class.java.name).setLevel(Level.FINE)

    Application.launch(KomaApp::class.java, *args)
    appState.stopSync?.invoke()
}


@ExperimentalCoroutinesApi
class KomaApp : App(StartScreen::class) {

    init {
        Thread.setDefaultUncaughtExceptionHandler(NoAlertErrorHandler())
        reloadStylesheetsOnFocus()
    }

    private fun load() {
        val args = parameters.raw
        val arg = args.firstOrNull()
        val data_dir = arg ?: getConfigDir()
        val s = AppStore(data_dir)
        appState.store = s
        val proxy = s.settings.proxyList.default()
        appState.koma = Koma(proxy.toJavaNet(), path = data_dir,
                addTrust = loadOptionalCert(File(data_dir)))
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
