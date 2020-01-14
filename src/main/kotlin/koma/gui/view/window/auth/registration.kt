package koma.gui.view.window.auth

import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.util.StringConverter
import koma.AuthFailure
import koma.Failure
import koma.matrix.user.auth.AuthType
import koma.matrix.user.auth.Register
import koma.matrix.user.auth.RegisterdUser
import koma.matrix.user.auth.Unauthorized
import koma.util.KResult
import koma.util.testFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.database.models.KDataStore
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.gui.alert
import mu.KotlinLogging
import okhttp3.HttpUrl
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

class RegistrationWizard(
        data: KDataStore,
        private val keyValueStore: KeyValueStore
) {

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
            val (newUser, ex, res) = cur.submit()?: return
            if (!res.testFailure(newUser, ex)) {
                println("Successfully registered ${newUser.user_id}")
                keyValueStore.userToToken.put(newUser.user_id.full, newUser.access_token)
                val s = Success(newUser, register.server, this)
                state = s
                uilaunch { root.center = s.root }
            } else {
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
        TODO()
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
       TODO()
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
