package koma.gui.view.usersview

import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import koma.gui.element.control.PrettyListView
import koma.gui.view.usersview.fragment.MemberCell
import koma.matrix.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.StyleBuilder
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.em
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.view.AccountContext

@ExperimentalCoroutinesApi
class RoomMemberListView(
        context: AccountContext,
        userData: UserDataStore
) {

    val root = VBox(5.0)
    private val userlist = PrettyListView<UserId>()
    fun setList(memList: ObservableList<UserId>){
        userlist.items = memList
    }
    init {
        with(root) {
            userlist.apply {
                isFocusTraversable = false
                style = listStyle
                VBox.setVgrow(this, Priority.ALWAYS)
                setCellFactory {
                    MemberCell(context, userData)
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
