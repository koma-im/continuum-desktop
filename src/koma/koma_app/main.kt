package koma.koma_app

import javafx.application.Application
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.Koma
import koma.gui.save_win_geometry
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import koma_app.appState
import tornadofx.*
import util.getConfigDir
import kotlinx.coroutines.javafx.JavaFx as UI



fun main(args: Array<String>) {
    val arg = args.firstOrNull()
    val data_dir = arg ?: getConfigDir()
    appState.koma = Koma(data_dir)
    Application.launch(KomaApp::class.java, *args)
    appState.chatController.shutdown()
    SaveJobs.finishUp()
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
