package koma.gui.view

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.util.StringConverter
import koma.controller.requests.account.login.onClickLogin
import koma.gui.view.window.auth.RegistrationWizard
import koma.gui.view.window.preferences.PreferenceWindow
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import link.continuum.database.KDataStore
import link.continuum.database.models.getRecentUsers
import link.continuum.database.models.getServerAddrs
import link.continuum.desktop.gui.whiteBackGround
import mu.KotlinLogging
import okhttp3.HttpUrl
import org.controlsfx.control.decoration.Decoration
import org.controlsfx.control.textfield.TextFields
import org.controlsfx.validation.Severity
import org.controlsfx.validation.ValidationMessage
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import org.controlsfx.validation.decoration.GraphicValidationDecoration
import tornadofx.*


private val logger = KotlinLogging.logger {}
private val settings: AppSettings = appState.store.settings

/**
 * Created by developer on 2017/6/21.
 */
class LoginScreen(
        private val data: KDataStore = appState.store.database
): View(), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    override val root = VBox()

    var userId = ComboBox<UserId>().apply {
        promptText = "@user:matrix.org"
        isEditable = true
        converter = object : StringConverter<UserId>() {
            override fun toString(u: UserId): String =u.str
            override fun fromString(string: String): UserId = UserId(string)
        }
    }
    var serverCombo= TextFields.createClearableTextField().apply {
        promptText = "https://matrix.org"
    }
    var password = TextFields.createClearablePasswordField()

    private val prefWin by lazy { PreferenceWindow() }
    private val validation = ValidationSupport().apply {
        validationDecorator = object: GraphicValidationDecoration(){
            override fun createDecorationNode(message: ValidationMessage?): Node {
                val graphic = if (Severity.ERROR === message?.getSeverity()) createErrorNode() else createWarningNode()
                graphic.style = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);"
                val label = Label()
                label.setGraphic(graphic)
                label.setTooltip(createTooltip(message))
                label.setAlignment(Pos.TOP_CENTER)
                return label
            }

            override fun createRequiredDecorations(target: Control?): MutableCollection<Decoration> {
                return mutableListOf()
            }
        }
    }
    init {
        val iconstream = javaClass.getResourceAsStream("/icon/koma.png");
        if (iconstream != null) {
            FX.primaryStage.icons.add(Image(iconstream))
        } else {
            logger.error { "Failed to load app icon from resources" }
        }

        val grid = GridPane()
        with(grid) {
            vgap = 10.0
            hgap = 10.0
            paddingAll = 5.0

            add(Text("Username"), 0, 0)
            val recentUsers = getRecentUsers(data)
            userId.itemsProperty().get().setAll(recentUsers)
            add(userId, 1, 0)
            add(Text("Server"), 0, 1)
            add(serverCombo, 1,1)
            add(Text("Password"), 0, 2)
            password = PasswordField()
            add(password, 1, 2)

            // TODO not updated in real time when editing
            validation.registerValidator(userId, Validator.createPredicateValidator({ p0: UserId? ->
                p0 ?: return@createPredicateValidator true
                return@createPredicateValidator p0.user.isNotBlank() && p0.server.isNotBlank()
            }, "User ID should be of the form @user:matrix.org"))
            validation.registerValidator(serverCombo, Validator.createPredicateValidator({ s: String? ->
                s?.let { HttpUrl.parse(it) } != null
            }, "Server should be a valid HTTP/HTTPS URL"))
        }
        with(root) {
            background = whiteBackGround
            style {
                fontSize= settings.scaling.em
            }
            val buts = HBox(10.0).apply {
                button("Options") {
                    action { prefWin.openModal() }
                }
                button("Register") {
                    action {
                        val o2 = FX.primaryStage.scene.root
                        openInternalWindow(RegistrationWizard(data), owner = o2)
                    }
                }
                hbox { hgrow = Priority.ALWAYS }
                button("Login") {
                    isDefaultButton = true
                    this.disableProperty().bind(validation.invalidProperty())
                    action {
                        GlobalScope.launch {
                            val k = appState.koma
                            val d = appState.store
                            onClickLogin(k, d, userId.value, password.text, serverCombo.text)
                        }
                    }
                }
            }
            stackpane {
                children.addAll(
                        StackPane().apply {
                            effect = DropShadow(59.0, Color.GRAY)
                            background = whiteBackGround
                        },
                        VBox(10.0).apply {
                            padding = Insets(10.0)
                            background = whiteBackGround
                            children.addAll(grid, buts)
                        })
            }
        }
        val userInput = Channel<String>(Channel.CONFLATED)
        userId.editor.textProperty().addListener { _, _, newValue ->
            userInput.offer(newValue)
        }
        launch(Dispatchers.Default) {
            for (u in userInput) {
                val a = suggestedServerAddr(data, UserId(u))
                if (userId.isFocused || serverCombo.text.isBlank()) {
                    withContext(Dispatchers.Main) {
                        serverCombo.text = a
                    }
                }
            }
        }
        userId.selectionModel.selectFirst()
    }
}

private fun suggestedServerAddr(data: KDataStore, userId: UserId): String {
    val sn = userId.server
    if (sn.isBlank()) return "https://matrix.org"
    getServerAddrs(data, sn).firstOrNull()?.let { return it }
    return "https://$sn"
}