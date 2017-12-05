package koma.koma_app

import javafx.application.Application
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.gui.save_win_geometry
import koma.gui.view.overlay.tooltip.style.TooltipStyle
import tornadofx.*
import view.LoginScreen
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

fun main(args: Array<String>) {
  Application.launch(KomaApp::class.java, *args)
}


class KomaApp : App(LoginScreen::class, TooltipStyle::class) {

    init {
        reloadStylesheetsOnFocus()
    }

  override fun start(stage: Stage) {
      super.start(stage)
      stage.onCloseRequest = EventHandler<WindowEvent> {
          save_win_geometry(stage)
      }
  }

}
