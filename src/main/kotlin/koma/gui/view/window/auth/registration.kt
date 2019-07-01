package koma.gui.view.window.auth

import javafx.scene.control.Alert
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import koma.koma_app.appState
import koma.matrix.user.auth.*
import koma.util.*
import koma.util.KResult
import koma.util.KResult as Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.database.KDataStore
import link.continuum.database.models.saveServerAddr
import link.continuum.database.models.saveToken
import link.continuum.desktop.action.startChat
import link.continuum.desktop.gui.uialert
import link.continuum.desktop.util.Err
import link.continuum.desktop.util.Ok
import okhttp3.HttpUrl
import tornadofx.*
import kotlin.onFailure

class RegistrationWizard(private val data: KDataStore): View() {

    override val root = BorderPane()
    private var state: WizardState = Start(data)
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
        if (cur is Start) {
            val (r, u) = cur.start()?:return
            register = r
            GlobalScope.launch(Dispatchers.JavaFx) {
                val a = Stage(r, u)
                state = a
                root.center = a.root
            }
        }
        else if (cur is Stage) {
            val res = cur.submit()?: return
            res.onSuccess { newUser ->
                println("Successfully registered ${newUser.user_id}")
                val k = appState.store.database
                saveToken(k, newUser.user_id, newUser.access_token)
                val s = Success(newUser, register.server, this)
                state = s
                uilaunch { root.center = s.root }
            }
            res.onFailure { ex ->
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
        val options: List<AuthType> = unauthorized.flows
                .mapNotNull { flow -> flow.stages.firstOrNull() }
                .map { AuthType.parse(it) }
        with(root) {
            top {
                hbox {
                    label("Next step, continue with: ")
                    hbox { hgrow = Priority.ALWAYS }
                    combobox(values = options) {
                        converter = object : StringConverter<AuthType>() {
                            // This is not going to be called
                            // because the ComboBox is editable
                            override fun fromString(string: String?): AuthType {
                                return AuthType.parse(string ?: "error")
                            }

                            override fun toString(item: AuthType): String {
                                return item.toDisplay()
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

    private fun switchAuthType(type: AuthType) {
        println("Switching to auth type $type")
        val a: AuthView = when (type) {
            is AuthType.Dummy -> PasswordAuthView(register, unauthorized)
            else -> FallbackWebviewAuth(register, unauthorized, type.type)
        }
        authView = a
        root.center = a.root
    }
}

private class Success(user: RegisterdUser, server: HttpUrl, window: RegistrationWizard): WizardState() {

    override val root = HBox()
    init {
        with(root) {
            vbox {
                label("Registration Success!")
                label(user.user_id.toString())
                button("Login Now") {
                    action {
                        window.close()
                        val k = appState.koma
                        startChat(k, user.user_id, user.access_token, server, appState.store)
                    }
                }
            }
        }
    }
}

abstract class AuthView: View() {
    /**
     * if it returns non-null value, the stage is none
     * if the result is ok, registration is finished
     * if it is Unauthorized, more stages are needed
     * if it's other exceptions, registration has failed
     */
    abstract suspend fun finish(): KResult<RegisterdUser, Exception>?
}

/**
 * Register by setting a password
 */
class PasswordAuthView(
        private val register: Register,
        private val unauthorized: Unauthorized
): AuthView() {
    /**
     * if it returns non-null value, the stage is none
     * if the result is ok, registration is finished
     * if it is Unauthorized, more stages are needed
     * if it's other exceptions, registration has failed
     */
    override suspend fun finish(): KResult<RegisterdUser, Exception>? {
        val f = register.registerByPassword(user.text, pass.text).getFailureOr { return Ok(it) }
        if (f is AuthException.AuthFail) return Err(f)
        uilaunch {
            alert(Alert.AlertType.WARNING,
                    "Registration hasn't succeeded",
                    "Error: ${f.message}"
            )
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
    override val root = Label("under construction")
    private var webAuthDone = false
    override suspend fun finish(): KResult<RegisterdUser, Exception>? {
        if (webAuthDone) {
            TODO("not sure whether there is a fallback method for registration")
        } else {
            uilaunch {
                alert(Alert.AlertType.ERROR, "Step not done yet",
                        "Please complete the step in the displayed web page")
            }
            return null
        }
    }
    init {
        val url = register.server.newBuilder()
                .addPathSegments("_matrix/media/r0/download")
                .addEncodedPathSegment("auth")
                .addEncodedPathSegment(type)
                .addEncodedPathSegment("fallback")
                .addEncodedPathSegment("web")
                .addQueryParameter("session", unauthorized.session)
                .build()
        println("Using fallback webpage authentication: $url")
    }
}

private class Start(private val data: KDataStore): WizardState() {
    suspend fun start(): Pair<Register, Unauthorized>? {
        val addr = serverCombo.editor.text
        val s = HttpUrl.parse(addr)
        if (s == null) {
            uialert(Alert.AlertType.ERROR, "$addr isn't valid server")
            return null
        }
        saveServerAddr(data, s.host(), addr)
        val r = Register(s, appState.koma.http)
        val f = r.getFlows() getOr {
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR,
                        "Failed to get authentication flows from server: ${it}")
            }
            return null
        }
        return r to f
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

fun uilaunch(eval: suspend CoroutineScope.()->Unit) {
    GlobalScope.launch(Dispatchers.JavaFx, block = eval)
}
