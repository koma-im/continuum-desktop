package koma.gui.view.usersview

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import koma.ui.PrettyListView
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.usersview.fragment.get_cell_property
import koma.model.user.UserState
import tornadofx.*

class RoomMemberListView(memList: ObservableList<UserState>): View() {

    override val root = VBox(10.0)

    init {
        with(root) {
            val showavataronly = SimpleBooleanProperty(true)
            val expandicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_LEFT)
            val collapseicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_RIGHT)
            val toggleicon = objectBinding(showavataronly) { if (value) expandicon else collapseicon}
            button {
                graphicProperty().bind(toggleicon)
                action {
                    showavataronly.set(showavataronly.not().get())
                }
            }
            val userlist = PrettyListView<UserState>()
            userlist.apply {
                items = memList
                vgrow = Priority.ALWAYS
                minWidth = 50.0
                val ulwidth = doubleBinding(showavataronly) {if(value) 50.0 else 138.0}
                maxWidthProperty().bind(ulwidth)
                prefWidthProperty().bind(ulwidth)
                cellFactoryProperty().bind(get_cell_property(showavataronly))
            }
            add(userlist)
        }
    }
}
