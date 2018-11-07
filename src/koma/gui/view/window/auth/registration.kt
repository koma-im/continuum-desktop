package koma.gui.view.window.auth

import com.github.kittinunf.result.Result
import javafx.scene.control.Alert
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import koma.matrix.user.auth.Register
import koma.matrix.user.auth.Unauthorized
import koma.storage.config.server.configServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*

class RegistrationWizard(): View() {

    override val root = BorderPane()
    private var current: RegisterWizardView = ServerSelection()
    init {
        title = "Join the Matrix network"

        with(root) {
            center = current.root
            bottom {
                borderpane {
                    right {
                        button("OK") { action { nextStage() } }
                    }
                }

            }
        }
    }
    private fun nextStage() {
        val cur = current
        when (cur) {
            is ServerSelection -> {
                GlobalScope.launch {
                    val (r, u) = cur.start()?:return@launch
                    GlobalScope.launch(Dispatchers.JavaFx) {
                        root.center = AuthStageView(r, u).root
                    }
                }
            }
            is AuthStageView -> {}
        }
    }
}

sealed class RegisterWizardView(): View()

class AuthStageView(
        private val register: Register,
        private val unauthorized: Unauthorized): RegisterWizardView() {
    override val root = Label("next stage")

    suspend fun submit() {}
}

class ServerSelection(): RegisterWizardView() {
    suspend fun start(): Pair<Register, Unauthorized>? {
        val addr = serverCombo.editor.text
        val s = configServerAddress(addr)
        if (s == null) {
            alert(Alert.AlertType.ERROR, "$addr isn't valid server")
            return null
        }
        val r = Register(s)
        val f = r.getFlows()
        when (f) {
            is Result.Failure -> {
                alert(Alert.AlertType.ERROR,
                        "Failed to get authentication flows from server: ${f.error}")
                return null
            }
            is Result.Success -> return Pair(r, f.value)
        }
    }

    private val serverCombo: ComboBox<String>

    override val root = VBox()
    init {
        with(root) {
            val serverCommonUrls = listOf("https://matrix.org")
            serverCombo = combobox(values = serverCommonUrls) {
                isEditable = true
                selectionModel.select(0)
            }
            add(serverCombo)
        }
    }
}
