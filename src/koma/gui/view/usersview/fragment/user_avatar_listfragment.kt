package koma.gui.view.usersview.fragment

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import koma.gui.element.icon.AvatarAlways
import koma.koma_app.appState
import koma.model.user.UserState
import koma.storage.persistence.settings.AppSettings
import tornadofx.*

private val settings: AppSettings = appState.store.settings

class MemberCell(private val showNoName: SimpleBooleanProperty) : ListCell<UserState>() {
    private val root = HBox( 5.0)
    val avatarPane: StackPane
    val name: Label
    init {
        val scale = settings.scaling
        val avsize = scale * 32.0

        root.apply {
            minWidth = 1.0
            prefWidth = 1.0
            style {
                alignment = Pos.CENTER_LEFT
                fontSize= scale.em
            }
            avatarPane = stackpane {
                minHeight = avsize
                minWidth = avsize
            }

            name = label() {
                removeWhen(showNoName)
            }
        }
    }
    override fun updateItem(item: UserState?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        avatarPane.children.setAll(AvatarAlways(item.avatar, item.name, item.color))
        name.text = item.name.get()
        name.textFill = item.color
        graphic = root
    }
}
