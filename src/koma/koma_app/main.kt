package koma.koma_app

import controller.events
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.WindowEvent
import koma.gui.save_win_geometry
import tornadofx.App
import view.LoginScreen

fun main(args: Array<String>) {
  Application.launch(KomaApp::class.java, *args)
}


class KomaApp : App(LoginScreen::class) {

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
