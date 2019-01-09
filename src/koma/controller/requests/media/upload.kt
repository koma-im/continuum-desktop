package koma.controller.requests.media

import com.github.kittinunf.result.Result
import javafx.scene.control.Alert
import koma.matrix.MatrixApi
import koma.matrix.UploadResponse
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.file.guessMediaType
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
suspend fun uploadFile(api: MatrixApi, file: File, filetype: MediaType? = null): Result<UploadResponse, Exception> {
    val type = filetype ?: file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val uploadResult = api.uploadFile(file, type).awaitMatrix()
    if (uploadResult is Result.Failure ) {
        val ex = uploadResult.error
        val error = "during upload: error ${ex.message}"
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, error)
        }
    }
    return uploadResult
}
