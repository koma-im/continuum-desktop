package koma.gui.view.window.preferences.tab

import javafx.scene.Parent
import javafx.scene.control.ComboBox
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import tornadofx.*

class AppearanceTab(
        parent: View,
        private val settings: AppSettings = appState.store.settings
): View() {
    override val root: Parent = Fieldset()

    val scalingSetting: ComboBox<String>

    init {
        val scales = listOf(settings.scaling, 1.0, 2.0)
                .map {
                    String.format("%.2f", it)
                }
                .distinct()
        scalingSetting = combobox(values = scales) {
            isEditable = true
            selectionModel.select(0)
        }

        val valid = booleanBinding(scalingSetting.editor.textProperty()) {
            value?.toFloatOrNull()?.let { it > 0.5 && it < 4.0 } ?: false
        }
        with(root){
            form {
                fieldset("Appearance") {
                    field("Scaling (Needs restart)") {
                        add(scalingSetting)
                    }
                }
            }
            buttonbar {
                button("Ok") {
                    disableWhen {
                        !valid
                    }
                    action {
                        save()
                        parent.close()
                    }
                }
            }
        }
    }

    private fun save() {
        scalingSetting.value?.toFloat()?.let {
            settings.scaling = it
        }
    }
}
