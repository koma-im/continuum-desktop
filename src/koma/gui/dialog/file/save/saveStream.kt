package koma.gui.dialog.file.save

import javafx.scene.control.Alert
import javafx.stage.FileChooser
import koma.network.media.saveUrlToFile
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import okhttp3.HttpUrl
import tornadofx.*

fun downloadFileAs(url: HttpUrl, filename: String, title: String = "Save file as") {
    val dialog = FileChooser()
    dialog.title = title
    dialog.initialFileName = filename

    val file = dialog.showSaveDialog(FX.primaryStage)
    file?:return

    launch {
        if (!saveUrlToFile(url, file))
            launch(JavaFx) {
                alert(Alert.AlertType.ERROR,
                        "Something went wrong while downloading file",
                        "Source: $url\nDestination: $file")
            }
    }
}
