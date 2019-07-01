package koma.controller.requests.media

import javafx.scene.control.Alert
import koma.matrix.MatrixApi
import koma.matrix.UploadResponse
import koma.util.KResult
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.file.guessMediaType
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import okhttp3.MediaType
import tornadofx.*
import java.io.File

/**
 * upload file, displays alerts if failed
 */
suspend fun uploadFile(api: MatrixApi, file: File, filetype: MediaType? = null): KResult<UploadResponse, Exception> {
    val type = filetype ?: file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val uploadResult = api.uploadFile(file, type).awaitMatrix()
    uploadResult.onFailure { ex ->
        val error = "during upload: error ${ex.message}"
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, error)
        }
    }
    return uploadResult
}
