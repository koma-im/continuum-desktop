package koma.gui.view.usersview

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.Server
import koma.gui.element.control.PrettyListView
import koma.gui.view.usersview.fragment.MemberCell
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.list.user.UserDataStore

@ExperimentalCoroutinesApi
class RoomMemberListView(
        userData: UserDataStore
) {

    val root = VBox(5.0)
    private val userlist = PrettyListView<Pair<UserId, Server>>()
    fun setList(memList: ObservableList<Pair<UserId, Server>>){
        userlist.items = memList
    }
    init {
        with(root) {
            userlist.apply {
                isFocusTraversable = false
                style = listStyle
                VBox.setVgrow(this, Priority.ALWAYS)
                setCellFactory {
                    MemberCell(userData)
                }
            }
            add(userlist)
        }
    }

    companion object {
        private val listStyle = StyleBuilder().apply {
            minWidth = 3.em
        }.toStyle()
    }
}
