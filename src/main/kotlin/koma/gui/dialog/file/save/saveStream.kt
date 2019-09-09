package koma.gui.dialog.file.save

import javafx.scene.control.Alert
import javafx.stage.FileChooser
import link.continuum.desktop.util.download.saveUrlToFile
import koma.koma_app.appState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.util.gui.alert
import okhttp3.HttpUrl

fun downloadFileAs(url: HttpUrl, filename: String = url.guessFileName(), title: String = "Save file as") {
    val dialog = FileChooser()
    dialog.title = title
    dialog.initialFileName = filename

    val file = dialog.showSaveDialog(JFX.primaryStage)
    file?:return
    GlobalScope.launch {
        if (!appState.koma.saveUrlToFile(url, file))
            launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR,
                        "Something went wrong while downloading file",
                        "Source: $url\nDestination: $file")
            }
    }
}

fun HttpUrl.guessFileName(): String {
    val ps = this.encodedPathSegments()
    val ls = ps.getOrNull(ps.lastIndex)
    return ls ?: ""
}
