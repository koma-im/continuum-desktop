package koma.gui.view.usersview

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.usersview.fragment.get_cell_property
import koma.model.user.UserState
import koma.storage.config.settings.AppSettings
import koma.ui.PrettyListView
import tornadofx.*
import kotlin.math.roundToInt

class RoomMemberListView(memList: ObservableList<UserState>): View() {

    override val root = VBox(10.0)

    init {
        with(root) {
            val scale = AppSettings.settings.scaling
            val size: String = "${scale.roundToInt()}em"
            style {
                fontSize= scale.em
            }
            val showavataronly = SimpleBooleanProperty(true)
            val expandicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_LEFT, size)
            val collapseicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_RIGHT, size)
            val toggleicon = objectBinding(showavataronly) { if (value) expandicon else collapseicon}
            button {
                graphicProperty().bind(toggleicon)
                action {
                    showavataronly.set(showavataronly.not().get())
                }
            }
            val userlist = PrettyListView<UserState>()
            userlist.apply {
                isFocusTraversable = false
                items = memList
                vgrow = Priority.ALWAYS
                minWidth = 50.0 * scale
                val ulwidth = doubleBinding(showavataronly) { scale * if(value) 50.0 else 138.0}
                maxWidthProperty().bind(ulwidth)
                prefWidthProperty().bind(ulwidth)
                cellFactoryProperty().bind(get_cell_property(showavataronly))
            }
            add(userlist)
        }
    }
}
