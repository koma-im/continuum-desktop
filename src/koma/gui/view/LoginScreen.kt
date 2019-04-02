package koma.gui.view

import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.control.PasswordField
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import koma.controller.requests.account.login.onClickLogin
import koma.gui.view.window.auth.RegistrationWizard
import koma.gui.view.window.preferences.PreferenceWindow
import koma.koma_app.appState
import koma.matrix.user.identity.UserId_new
import koma.storage.config.profile.getRecentUsers
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import tornadofx.*

private val logger = KotlinLogging.logger {}
private val settings: AppSettings = appState.store.settings

/**
 * Created by developer on 2017/6/21.
 */
class LoginScreen(): View() {

    override val root = VBox()

    var userId: ComboBox<String> by singleAssign()
    var serverCombo: ComboBox<String> by singleAssign()
    var password: PasswordField by singleAssign()

    init {
        title = "Koma"

        val iconstream = javaClass.getResourceAsStream("/icon/koma.png");
        if (iconstream != null) {
            FX.primaryStage.icons.add(Image(iconstream))
        } else {
            logger.error { "Failed to load app icon from resources" }
        }

        val grid = GridPane()
        with(grid) {
            paddingAll = 5.0
            row("Username") {
                val recentUsers = appState.koma.getRecentUsers().map { it.toString() }
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
                fontSize= settings.scaling.em
            }
            add(grid)

            val settings = PreferenceWindow()
            button("More Options") {
                action { settings.openModal() }
            }
            buttonbar {
                button("Register") {
                    action {
                        val o2 = FX.primaryStage.scene.root
                        openInternalWindow(RegistrationWizard(), owner = o2)
                    }
                }
                button("Login") {
                    isDefaultButton = true
                    action {
                        GlobalScope.launch {
                            val k = appState.koma
                            val d = appState.store.database
                            onClickLogin(k, d, userId.value, password.text, serverCombo.editor.text)
                        }
                    }
                }
            }
        }
        userId.selectionModel.selectedItem?.let { setServerAddr(it) }
        userId.selectionModel.selectedItemProperty().onChange { it?.let { setServerAddr(it) } }
    }

    private fun setServerAddr(input: String){
        val id = UserId_new(input)
        val addrs = appState.koma.servers.serverConf(id.server).addresses
        serverCombo.items = FXCollections.observableArrayList(addrs)
        serverCombo.selectionModel.selectFirst()
    }
}
