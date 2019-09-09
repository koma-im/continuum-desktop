package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.control.Hyperlink
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseButton
import javafx.scene.text.Font
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.JFX
import java.io.IOException


fun hyperlinkNode(
        text: String,
        settings: AppSettings = appState.store.settings
): Hyperlink {
    val node = Hyperlink(text)
    val tip = Tooltip(text)
    tip.font = Font.font(settings.fontSize)
    node.tooltip = tip
    node.setOnMouseClicked { e ->
        if (e.button == MouseButton.PRIMARY) openInBrowser(text)
    }
    return node
}

fun openInBrowser(url: String) {
    if (tryExecBrowsers(url)) return
    JFX.hostServices.showDocument(url)
}

val browserExes = listOf(
        "xdg-open", "x-www-browser", "firefox",
        "chrome", "chromium", "opera", "midori",
        "epiphany"
)

fun tryExecBrowsers(url: String): Boolean {
    for (browserExe in browserExes) {
        try {
            val procb = ProcessBuilder(browserExe, url)
            // TODO stdout and stderr of sub process should be discarded
            procb.start()
            return true
        } catch (e: IOException) {
            System.err.println("failed to launch browser $browserExe $url")
            e.printStackTrace()
        }
    }
    return false
}
