package koma.gui.view.usersview

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text
import koma.Server
import koma.gui.element.control.PrettyListView
import koma.gui.view.usersview.fragment.MemberCell
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.doubleBinding
import link.continuum.desktop.gui.list.user.UserDataStore
import tornadofx.style
import tornadofx.em

private val settings: AppSettings = appState.store.settings

@ExperimentalCoroutinesApi
class RoomMemberListView(
        userData: UserDataStore
) {

    val root = VBox(5.0).apply {
        padding = Insets(5.0)
    }
    private val userlist = PrettyListView<Pair<UserId, Server>>()
    fun setList(memList: ObservableList<Pair<UserId, Server>>){
        userlist.items = memList
    }
    init {
        with(root) {
            val scale = settings.scaling
            val showavataronly = SimpleBooleanProperty(true)
            val button  = Pane().apply {
                fun Text.iconProp(): Text {
                    opacity = .5
                    fill = Color.FORESTGREEN
                    textOrigin = VPos.TOP
                    return this
                }
                val size = "${scale*13.5}px"
                val expandIcon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PLUS_CIRCLE, size).iconProp()
                val collapseIcon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.MINUS_CIRCLE, size).iconProp()
                children.setAll(expandIcon)
                setOnMouseClicked {
                    if (it.button != MouseButton.PRIMARY) return@setOnMouseClicked
                    val compact = showavataronly.get()
                    children.setAll(if (compact) collapseIcon else expandIcon)
                    showavataronly.set(!compact)
                }
            }
            add(button)
            userlist.apply {
                isFocusTraversable = false
                VBox.setVgrow(this, Priority.ALWAYS)
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
