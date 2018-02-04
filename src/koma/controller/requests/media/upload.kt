package koma.controller.requests.media

import domain.UploadResponse
import javafx.scene.control.Alert
import kotlinx.coroutines.experimental.launch
import matrix.ApiClient
import okhttp3.MediaType
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*
import java.io.File

/**
 * upload file, displays alerts if failed
 */
suspend fun uploadFile(api: ApiClient, file: File, type: MediaType): Result<UploadResponse> {
    val uploadResult = api.uploadFile(file, type).awaitResult()
    when (uploadResult ) {
        is Result.Error -> {
            val error = "during upload: http error ${uploadResult.exception.code()}: ${uploadResult.exception.message()}"
            launch(kotlinx.coroutines.experimental.javafx.JavaFx) {
                alert(Alert.AlertType.ERROR, error)
            }
        }
        is Result.Exception -> {
            val ex = uploadResult.exception
            launch(kotlinx.coroutines.experimental.javafx.JavaFx) {
                alert(Alert.AlertType.ERROR, "during upload exception $ex, ${ex.cause}")
            }
        }
    }
    return uploadResult
}
