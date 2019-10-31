package link.continuum.desktop.gui.view

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.input.MouseButton
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.shape.Ellipse
import javafx.scene.shape.FillRule
import javafx.scene.shape.SVGPath
import javafx.scene.shape.Shape
import koma.gui.view.usersview.RoomMemberListView
import koma.koma_app.AppStore
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.notification.NotificationList
import link.continuum.desktop.util.Account
import model.Room

class RightColumn(
        account: Account,
        private val storage: AppStore,
        private val parent: SplitPane
) {
    private val members = RoomMemberListView(storage.userData)
    private val notifications by lazy { NotificationList(account, storage).apply {
        updateServer(account.server)
    }}
    private val content = StackPane(members.root)
    private val tabs = HBox(
            HBox(Shape.union(
                    SVGPath().apply {
                        content = "M13,14 L13,12.4444444 C13,10.7262252 11.5449254,9.33333333 9.75,9.33333333 L3.25,9.33333333 C1.45507456,9.33333333 0,10.7262252 0,12.4444444 L0,14"
                    },
                    Ellipse(6.5, 3.11111111 ,3.25,3.11111111)
            ).apply {
                scaleX = 1.5
                scaleY = 1.5
                fill = Color.GRAY
            }).apply {
                alignment = Pos.CENTER
                setOnMouseClicked {
                    if (it.button != MouseButton.PRIMARY) return@setOnMouseClicked
                    it.consume()
                    content.children.setAll(members.root)
                }
            },
            HBox(run {
                SVGPath().apply {
                    content = "M16,12 L0,12 C1.3254834,12 2.4,10.9254834 2.4,9.6 L2.4,5.6 C2.40000005,2.50720543 4.90720543,8.34465016e-08 8,8.3446502e-08 C11.0927946,8.34465023e-08 13.6,2.50720543 13.6,5.6 L13.6,9.6 C13.6,10.9254834 14.6745166,12 16,12 Z" +"M9.384,15.2 C9.09776179,15.6934435 8.57045489,15.997165 8,15.997165 C7.42954511,15.997165 6.90223821,15.6934435 6.616,15.2 L9.384,15.2 Z"
                    fill = Color.TRANSPARENT
                    strokeWidth = 2.0
                    stroke = Color.GRAY
                    scaleX = 1.35
                    scaleY = 1.35
                }
            }).also {
                it.alignment = Pos.CENTER
                it.setOnMouseClicked { ev ->
                    if (ev.button != MouseButton.PRIMARY) return@setOnMouseClicked
                    ev.consume()
                    content.children.setAll(notifications.root)
                    expandOnce()
                }
            }).apply {
        style = StyleBuilder().apply { minHeight = 2.em }.toStyle()
        children.forEach { HBox.setHgrow(it, Priority.ALWAYS)}
    }
    val root = VBox(0.0, tabs, content)
    fun setRoom(room: Room) {
        members.setList(room.members.list)
        notifications.updateServer(room.account.server)
    }
    private var expanded = false
    private fun expandOnce() {
        if (expanded) return
        expanded = true
        if (parent.dividerPositions.getOrNull(1)?.let {it > .88} == true) {
            parent.setDividerPosition(1, .8)
        }
    }
    init {
        VBox.setVgrow(content, Priority.ALWAYS)
    }
}