package koma.koma_app

import javafx.application.Application
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.Koma
import koma.gui.save_win_geometry
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import koma.storage.config.ConfigPaths
import koma.storage.config.getConfigDir
import okhttp3.OkHttpClient
import tornadofx.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.javafx.JavaFx as UI



fun main(args: Array<String>) {
    Logger.getLogger(OkHttpClient::class.java.name).setLevel(Level.FINE)
    val arg = args.firstOrNull()
    val data_dir = arg ?: getConfigDir()
    val paths = ConfigPaths(data_dir)
    appData = DataOnDisk(paths)
    val proxy = appData.settings.getProxy()
    val koma = Koma(paths, proxy)
    appState.koma = koma
    Application.launch(KomaApp::class.java, *args)
    appState.chatController.shutdown()
    SaveToDiskTasks.saveToDisk()
}


class KomaApp : App(StartScreen::class) {

    init {
        Thread.setDefaultUncaughtExceptionHandler(NoAlertErrorHandler())
        reloadStylesheetsOnFocus()
    }

  override fun start(stage: Stage) {
      super.start(stage)
      setSaneStageSize(stage)
      stage.hide()
      stage.show()
      stage.onCloseRequest = EventHandler<WindowEvent> {
          save_win_geometry(stage)
      }
  }

}
