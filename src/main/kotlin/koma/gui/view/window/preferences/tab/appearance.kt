package koma.gui.view.window.preferences.tab

import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ComboBox
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.booleanBinding
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.button
import link.continuum.desktop.gui.disableWhen
import tornadofx.*

class AppearanceTab(
        parent: View,
        private val settings: AppSettings = appState.store.settings
) {
    val root = Fieldset()

    val scalingSetting: ComboBox<String>

    init {
        val scales = listOf(settings.scaling, 1.0, 2.0)
                .map {
                    String.format("%.2f", it)
                }
                .distinct()
        scalingSetting = ComboBox<String>().apply {
            itemsProperty()?.value?.addAll(scales)
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
                        inputs.add(scalingSetting)
                    }
                }
            }
            add(ButtonBar().apply {
                button("Ok") {
                    disableWhen(!valid)
                    setOnAction {
                        save()
                        parent.close()
                    }
                }
            })
        }
    }

    private fun save() {
        scalingSetting.value?.toFloat()?.let {
            settings.scaling = it
        }
    }
}
