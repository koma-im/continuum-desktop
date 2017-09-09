package koma.koma_app

import controller.events
import koma.gui.view.overlay.tooltip.style.TooltipStyle
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.gui.save_win_geometry
import tornadofx.App
import tornadofx.reloadStylesheetsOnFocus
import view.LoginScreen

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
          it.consume()
          save_win_geometry(stage)
          events.beforeShutdownHook.forEach { it.invoke() }
          Platform.exit()
      }
  }

}
