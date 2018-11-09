package koma.gui.view.window.auth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import javafx.scene.control.Alert
import javafx.scene.control.ComboBox
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.util.StringConverter
import koma.matrix.user.auth.AuthException
import koma.matrix.user.auth.Register
import koma.matrix.user.auth.RegisterdUser
import koma.matrix.user.auth.Unauthorized
import koma.storage.config.server.configServerAddress
import koma.storage.config.server.getApiUrlBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import netscape.javascript.JSObject
import tornadofx.*

class RegistrationWizard(): View() {

    override val root = BorderPane()
    private var state: WizardState = Start()
    lateinit var register: Register
    init {
        title = "Join the Matrix network"

        with(root) {
            center = state.root
            bottom {
                borderpane {
                    right {
                        button("OK") { action { GlobalScope.launch { nextStage() } } }
                    }
                }

            }
        }
    }
    private suspend fun nextStage() {
        val cur = state
        when (cur) {
            is Start -> {
                val (r, u) = cur.start()?:return
                register = r
                GlobalScope.launch(Dispatchers.JavaFx) {
                    val a = Stage(r, u)
                    state = a
                    root.center = a.root
                }
            }
            is Stage -> {
                val res = cur.submit()?: return
                res.success { newUser ->
                    println("Successfully registered $newUser")
                }
                res.failure { ex ->
                    when (ex) {
                        is AuthException.AuthFail -> {
                            GlobalScope.launch(Dispatchers.JavaFx) {
                                val a = Stage(register, ex.status)
                                state = a
                                root.center = a.root
                            }
                        }
                        else -> {
                            GlobalScope.launch(Dispatchers.JavaFx) {
                                alert(Alert.AlertType.ERROR, "Registration failed",
                                        "Error $ex")
                                this@RegistrationWizard.close()
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class WizardState(): View()

class Stage(
        private val register: Register,
        private val unauthorized: Unauthorized): WizardState() {
    override val root = BorderPane()
    private var authView: AuthView? = null
    suspend fun submit(): Result<RegisterdUser, Exception>? {
        return authView?.finish()
    }

    init {
        val options = unauthorized.flows.map { flow -> flow.stages.first() }
        with(root) {
            top {
                hbox {
                    label("Next step, continue with: ")
                    hbox { hgrow = Priority.ALWAYS }
                    combobox(values = options) {
                        converter = object : StringConverter<String>() {
                            // This is not going to be called
                            // because the ComboBox is editable
                            override fun fromString(string: String?): String {
                                return "Error: Unexpected"
                            }

                            override fun toString(item: String): String {
                                return authTypeToDisplay(item)
                            }
                        }
                        selectionModel.selectedItemProperty().onChange { item ->
                            if (item != null) {
                                switchAuthType(item)
                            }
                        }
                        selectionModel.select(0)
                    }
                }
            }
        }
    }

    private fun switchAuthType(type: String) {
        println("Switching to auth type $type")
        val a: AuthView = when (type) {
            "m.login.dummy" -> PasswordAuthView(register, unauthorized, type)
            else -> FallbackWebviewAuth(register, unauthorized, type)
        }
        authView = a
        root.center = a.root
    }
}

abstract class AuthView: View() {
    /**
     * if it returns non-null value, the stage is none
     * if the result is ok, registration is finished
     * if it is Unauthorized, more stages are needed
     * if it's other exceptions, registration has failed
     */
    abstract suspend fun finish(): Result<RegisterdUser, Exception>?
}

/**
 * Register by setting a password
 */
class PasswordAuthView(
        private val register: Register,
        private val unauthorized: Unauthorized,
        private val type: String
): AuthView() {
    /**
     * if it returns non-null value, the stage is none
     * if the result is ok, registration is finished
     * if it is Unauthorized, more stages are needed
     * if it's other exceptions, registration has failed
     */
    override suspend fun finish(): Result<RegisterdUser, Exception>? {
        val result = register.registerByPassword(user.text, pass.text)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                when (result.error) {
                    is AuthException.AuthFail -> return result
                    else -> {
                        uilaunch {
                            alert(Alert.AlertType.WARNING,
                                    "Registration hasn't succeeded",
                                    "Error: ${result.error.message}"
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    override val root = Form()
    val user = textfield()
    val pass = passwordfield()

    init {
        with(root) {
            paddingAll = 5.0
            fieldset {
                field("Username") {
                    add(user)
                }
                field("Password") {
                    add(pass)
                }
            }
        }
    }
}

class FallbackWebviewAuth(
        private val register: Register,
        private val unauthorized: Unauthorized,
        private val type: String
): AuthView() {
    override val root = WebView()
    private var webAuthDone = false
    override suspend fun finish(): Result<RegisterdUser, Exception>? {
        if (webAuthDone) {
            return register.finishStage()
        } else {
            uilaunch {
                alert(Alert.AlertType.ERROR, "Step not done yet",
                        "Please complete the step in the displayed web page")
            }
            return null
        }
    }
    init {
        val url = register.serverConf.getApiUrlBuilder()!!
                .addEncodedPathSegment("auth")
                .addEncodedPathSegment(type)
                .addEncodedPathSegment("fallback")
                .addEncodedPathSegment("web")
                .addQueryParameter("session", unauthorized.session)
                .build()
        println("Using fallback webpage authentication: $url")
        root.engine.load(url.toString())
        val win = root.engine.executeScript("window") as JSObject

        class JavaApplication {
            fun finishAuth() {
                println("Auth is done")
                webAuthDone = true
            }
        }

        val app = JavaApplication()
        win.setMember("onAuthDone", "app.finishAuth()")
    }
}

private class Start(): WizardState() {
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
                GlobalScope.launch(Dispatchers.JavaFx) {
                    alert(Alert.AlertType.ERROR,
                            "Failed to get authentication flows from server: ${f.error}")
                }
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

private fun authTypeToDisplay(type: String): String {
    return when (type) {
        "m.login.dummy" -> "Password"
        "m.login.email.identity" -> "Email"
        else -> type
    }
}

fun uilaunch(eval: suspend CoroutineScope.()->Unit) {
    GlobalScope.launch(Dispatchers.JavaFx, block = eval)
}
