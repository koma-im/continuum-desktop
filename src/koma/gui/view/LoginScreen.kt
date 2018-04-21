package view

import controller.LoginController
import javafx.collections.FXCollections
import javafx.scene.control.Alert
import javafx.scene.control.ComboBox
import javafx.scene.control.PasswordField
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import koma.controller.requests.account.login.doLogin
import koma.controller.requests.account.registerUser
import koma.gui.view.window.preferences.PreferenceWindow
import koma.matrix.user.identity.UserId_new
import koma.matrix.user.identity.isUserIdValid
import koma.storage.config.profile.getRecentUsers
import koma.storage.config.server.loadServerConf
import koma.storage.config.settings.AppSettings
import rx.javafx.kt.toObservableNonNull
import rx.lang.kotlin.filterNotNull
import tornadofx.*
import kotlinx.coroutines.experimental.launch as corolaunch

/**
 * Created by developer on 2017/6/21.
 */
class LoginScreen(): View() {

    override val root = VBox()
    val controller = LoginController()

    var userId: ComboBox<String> by singleAssign()
    var serverCombo: ComboBox<String> by singleAssign()
    var password: PasswordField by singleAssign()

    init {
        title = "Koma"

        val iconstream = javaClass.getResourceAsStream("/icon/koma.png");
        FX.primaryStage.icons.add(Image(iconstream))

        val grid = GridPane()
        with(grid) {
            paddingAll = 5.0
            row("Username") {
                val recentUsers = getRecentUsers().map { it.toString() }
                userId = combobox(values = recentUsers) {
                    isEditable = true
                    selectionModel.selectFirst()

                }
            }
            row("Server") {
                val serverCommonUrls = listOf("https://matrix.org")
                serverCombo = combobox(values = serverCommonUrls) {
                    isEditable = true
                    selectionModel.select(0)
                }
            }
            row("Password") {
                password = passwordfield() {
                }
            }
        }
        with(root) {
            style {
                fontSize=AppSettings.settings.scaling.em
            }
            add(grid)

            val settings = PreferenceWindow()
            button("More Options") {
                action { settings.openModal() }
            }
            buttonbar {
                button("Register") {
                    action {
                        if (!isUserIdValid(userId.value)) {
                            alert(Alert.AlertType.WARNING, "Invalid user-id")
                        } else if (password.text.isBlank()) {
                            alert(Alert.AlertType.WARNING, "Empty password")
                        } else {
                            val userid = UserId_new(userId.value)
                            corolaunch {
                                registerUser(controller, userid, password.text, serverCombo.editor.text)
                            }
                        }
                    }
                }
                button("Login") {
                    isDefaultButton = true
                    action {
                        doLogin(userId.value, password.text, serverCombo.editor.text, controller)
                    }
                }
            }
        }

        set_up_listeners()
    }

    private fun set_up_listeners(){
        userId.selectionModel.selectedItemProperty().toObservableNonNull()
                            .map{ UserId_new(it) }
                            .filterNotNull()
                            .map { loadServerConf(it.server).addresses }
                            .filterNotNull()
                            .subscribe {
                                serverCombo.items = FXCollections.observableArrayList(it)
                                serverCombo.selectionModel.selectFirst()
                            }
    }
}
