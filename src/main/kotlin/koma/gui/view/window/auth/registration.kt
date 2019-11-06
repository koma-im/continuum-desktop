package koma.gui.view.window.auth

import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.util.StringConverter
import koma.AuthFailure
import koma.Failure
import koma.koma_app.Globals
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
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.util.Err
import link.continuum.desktop.util.Ok
import link.continuum.desktop.util.gui.alert
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

class RegistrationWizard(private val data: KDataStore) {

    val root = BorderPane()
    private var state: WizardState = Start(data)
    lateinit var register: Register
    init {

        with(root) {
            center = state.root
            bottom = BorderPane().apply {
                right = Button("OK").apply {
                    action { GlobalScope.launch { nextStage() } }
                } }
        }
    }
    fun close() {
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
                    is AuthFailure -> {
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

sealed class WizardState() {
    abstract val root: Parent
}

class Stage(
        private val register: Register,
        private val unauthorized: Unauthorized): WizardState() {
    override val root = BorderPane()
    private var authView: AuthView? = null
    suspend fun submit(): Result<RegisterdUser, Failure>? {
        return authView?.finish()
    }

    init {
        val options: List<AuthType> = unauthorized.flows
                .mapNotNull { flow -> flow.stages.firstOrNull() }
                .map { AuthType.parse(it) }
        with(root) {
            top = HBox().apply {
                label("Next step, continue with: ")
                add(HBox().apply { HBox.setHgrow(this, Priority.ALWAYS) })
                add(ComboBox<AuthType>().apply {
                    itemsProperty()?.value?.addAll(options)
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
                    selectionModel.selectedItemProperty().addListener { observable, oldValue, item ->
                        if (item != null) {
                            switchAuthType(item)
                        }
                    }
                    selectionModel.select(0)
                })
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
                    }
                }
            }
        }
    }
}

abstract class AuthView {
    abstract val root: Parent
    /**
     * if it returns non-null value, the stage is none
     * if the result is ok, registration is finished
     * if it is Unauthorized, more stages are needed
     * if it's other exceptions, registration has failed
     */
    abstract suspend fun finish(): KResult<RegisterdUser, Failure>?
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
    override suspend fun finish(): KResult<RegisterdUser, Failure>? {
        val (success, failure, result) = register.registerByPassword(user.text, pass.text)
        if (!result.testFailure(success, failure)) {
            return Ok(success)
        }
        val f = failure
        if (f is AuthFailure) return Err(f)
        uilaunch {
            alert(Alert.AlertType.WARNING,
                    "Registration hasn't succeeded",
                    "Error: $f"
            )
        }
        return null
    }

    override val root = GridPane()
    val user = TextField()
    val pass = PasswordField()

    init {
        with(root) {
            padding = Insets(5.0)
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
    override suspend fun finish(): KResult<RegisterdUser, Failure>? {
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
        logger.debug { "saved server addrs" }
        val r = Register(s, Globals.httpClient)
        logger.debug { "getting flows" }
        val f = r.getFlows()
        logger.debug { "got flows" }
        if (f.isFailure) {
            logger.debug { "flows isFailure" }
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR,
                        "Failed to get authentication flows from server: ${f}")
            }
            return null
        }
        return r to f.getOrThrow()
    }

    private val serverCombo: ComboBox<String>

    override val root = VBox()
    init {
        with(root) {
            serverCombo = ComboBox<String>().apply {
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
