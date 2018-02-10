package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.control.Alert
import javafx.scene.control.Hyperlink
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseButton
import javafx.scene.text.Font
import koma.storage.config.settings.AppSettings
import tornadofx.*
import java.awt.Desktop
import java.io.IOException
import java.net.URI


fun hyperlinkNode(text: String): Hyperlink {
    val node = Hyperlink(text)
    val tip = Tooltip(text)
    tip.font = Font.font(AppSettings.fontSize)
    node.tooltip = tip
    node.setOnMouseClicked { e ->
        if (e.button == MouseButton.PRIMARY) openInBrowser(text)
    }
    return node
}

fun openInBrowser(url: String) {
    val uri =try {
        URI.create(url)
    } catch (e: IllegalArgumentException) {
        alert(Alert.AlertType.ERROR, "Can't open address", "$url is not valid URI")
        return
    }
    if (!Desktop.isDesktopSupported()) {
        alert(Alert.AlertType.ERROR, "Desktop isn't supported" )
        return
    }
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.BROWSE)) {
        if (!tryExecBrowsers(url)) {
            alert(Alert.AlertType.ERROR,
                    "The desktop doesn't support browsing",
                   "Also, none of ${browserExes.joinToString(", ", limit = 10)} successfully starts" )
        }
        return
    }
    try {
        desktop.browse(uri)
    } catch (e: Exception) {
        e.printStackTrace()
    }
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
