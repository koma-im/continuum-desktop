package koma.gui.view.usersview

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.Server
import koma.gui.element.control.PrettyListView
import koma.gui.view.usersview.fragment.MemberCell
import koma.koma_app.appState
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.icon.avatar.SelectUser
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.OkHttpClient
import tornadofx.*
import kotlin.math.roundToInt

private val settings: AppSettings = appState.store.settings

@ExperimentalCoroutinesApi
class RoomMemberListView(
        userData: UserDataStore
): View() {

    override val root = VBox(10.0)
    private val userlist = PrettyListView<Pair<UserId, Server>>()
    fun setList(memList: ObservableList<Pair<UserId, Server>>){
        userlist.items = memList
    }
    init {
        with(root) {
            val scale = settings.scaling
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
            userlist.apply {
                isFocusTraversable = false
                vgrow = Priority.ALWAYS
                minWidth = 50.0 * scale
                val ulwidth = doubleBinding(showavataronly) { scale * if(value) 50.0 else 138.0}
                maxWidthProperty().bind(ulwidth)
                prefWidthProperty().bind(ulwidth)
                setCellFactory {
                    MemberCell(showavataronly, userData)
                }
            }
            add(userlist)
        }
    }
}
