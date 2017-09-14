package view

import controller.LoginController
import controller.LoginRequest
import controller.RegisterRequest
import controller.guiEvents
import javafx.collections.FXCollections
import javafx.scene.control.Alert
import javafx.scene.control.ComboBox
import javafx.scene.control.PasswordField
import javafx.scene.layout.GridPane
import koma.matrix.user.identity.UserId_new
import koma.storage.Recent
import matrix.UserRegistering
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import rx.javafx.kt.toObservableNonNull
import rx.lang.kotlin.filterNotNull
import tornadofx.*
import util.getRecentUsers

/**
 * Created by developer on 2017/6/21.
 */
class LoginScreen(): View() {

    override val root = GridPane()
    val controller = LoginController()

    var userId: ComboBox<String> by singleAssign()
    var serverCombo: ComboBox<String> by singleAssign()
    var password: PasswordField by singleAssign()

    init {
        title = "Koma"

        with(root) {
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
            row {
                buttonbar {
                    button("Register") {
                        actionEvents()
                                .map {
                                    val userid = UserId_new(userId.value)
                                    if ( userid == null) {
                                        alert(Alert.AlertType.WARNING, "Invalid user-id")
                                        null
                                    } else if (password.text.isBlank()) {
                                        alert(Alert.AlertType.WARNING, "Invalid password")
                                        null
                                    } else {
                                        RegisterRequest(
                                                serverCombo.editor.text,
                                                UserRegistering(
                                                        userid.user,
                                                        password.text
                                                )
                                        )
                                    }
                                }
                                .filterNotNull()
                                .addTo(guiEvents.registerRequests)
                    }
                    button("Login") {
                        isDefaultButton = true
                        actionEvents().map { UserId_new(userId.value) }.filterNotNull() .map {
                            LoginRequest(
                                    it,
                                    serverCombo.editor.text,
                                    if (password.text.isNotEmpty()) password.text else null)
                        }.addTo(guiEvents.loginRequests)
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
                            .map { Recent.server_addrs.get(it.server)}
                            .filterNotNull()
                            .subscribe {
                                serverCombo.items = FXCollections.observableArrayList(it)
                                serverCombo.selectionModel.selectFirst()
                            }
    }
}
