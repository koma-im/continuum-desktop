package koma.controller.requests.media

import javafx.scene.control.Alert
import koma.Failure
import koma.matrix.MatrixApi
import koma.matrix.UploadResponse
import koma.util.KResult
import koma.util.file.guessMediaType
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.util.gui.alert
import okhttp3.MediaType
import java.io.File

/**
 * upload file, displays alerts if failed
 */
suspend fun uploadFile(api: MatrixApi, file: File, filetype: MediaType? = null
): KResult<UploadResponse, Failure> {
    val type = filetype ?: file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val uploadResult = api.uploadFile(file, type)
    uploadResult.onFailure { ex ->
        val error = "during upload: error $ex"
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, error)
        }
    }
    return uploadResult
}
