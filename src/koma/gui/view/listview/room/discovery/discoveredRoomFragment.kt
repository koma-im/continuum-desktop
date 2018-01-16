package gui.view.listview.room.discovery

import domain.DiscoveredRoom
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.graphic.getImageForName
import koma.graphic.hashStringColorDark
import koma.gui.media.getMxcImagePropery
import tornadofx.*

class DiscoveredRoomItemModel(property: ObjectProperty<DiscoveredRoom>)
    : ItemViewModel<DiscoveredRoom>(itemProperty = property) {
    val name = bind {item?.name.toProperty()}
    val aliases = bind {item?.aliasesProperty()}
    val has_several_aliases = bind {item?.hasSeveralAliases() }
    val avatar = bind {item?.avatarProperty()}
    val guest = bind {item?.guest_can_join.toProperty()}
    val n_mems = bind {item?.num_joined_members.toProperty()}
    val room_id = bind {item?.room_id.toProperty()}
    val topic = bind {item?.topic.toProperty()}
    val world_read = bind {item?.world_readable.toProperty()}
}

fun DiscoveredRoom.avatarProperty(): SimpleObjectProperty<Image> {
    if (this.avatar_url != null)
        return getMxcImagePropery(this.avatar_url, 32.0, 32.0)
    else {
        val im = getImageForName(this.dispName(), hashStringColorDark(this.room_id))
        return SimpleObjectProperty(im)
    }
}

fun DiscoveredRoom.hasSeveralAliases(): SimpleBooleanProperty {
    val b= this.aliases?.isNotEmpty() ?: false
    return SimpleBooleanProperty(b)
}


class DiscoveredRoomFragment: ListCellFragment<DiscoveredRoom>() {

    val droom = DiscoveredRoomItemModel(itemProperty)
    val name = droom.name as SimpleStringProperty
    val topic = droom.topic as SimpleStringProperty
    val no_alias = booleanBinding(droom.aliases) { if (value != null) {value.size == 0 } else true}

    override val root = hbox(spacing = 10.0) {
        val tooltifrag = TooltipFragment(droom)
        tooltip(graphic = tooltifrag.root) {
        }
        imageview(droom.avatar)
        vbox  {
            // name
            hbox {
                removeWhen { name.isEmpty }
                text("Name:") {
                    style {
                        opacity = 0.5
                    }
                }
                text(name) {
                }
            }
            // alias(es)
            hbox(spacing = 5) {
                val has_aliases = droom.has_several_aliases
                removeWhen { no_alias }
                val label = stringBinding (has_aliases) { if (value) "Aliases:" else "Alias:   "}
                text(label) {
                    style {
                        opacity = 0.5
                    }
                }
                val alias = stringBinding(droom.aliases) {
                    if (value != null && value.isNotEmpty())
                        value.get(0)
                    else
                        "<unknown alias>"
                }
                text(alias)
                text(", ...") {
                    removeWhen { droom.has_several_aliases }
                }
            }
            // member count
            hbox (spacing = 5) {
                text("Members:") {
                    style { opacity = 0.5 }
                }
                text(stringBinding(droom.n_mems) { value.toString()})
            }
            // topic
            hbox {
                removeWhen { topic.isEmpty }
                text("Topic") {
                    style {
                        opacity = 0.5
                    }
                }
                text(topic)
            }
        }
    }
}

class TooltipFragment(room: DiscoveredRoomItemModel) : Fragment() {
    override val root = VBox()

    init {
        with(root) {
            vbox {
                removeWhen { booleanBinding(room.has_several_aliases) {!value} }
                label("Other Aliases") {
                    style {
                        opacity = 0.9
                        fontSize = 15.px
                    }
                }
                label(stringBinding(room.aliases) {
                    value?.drop(1)?.joinToString("\n") ?: ""}) {
                    paddingLeft = 5.0
                    style {
                        fontSize = 12.px
                    }
                }
            }
            label("Anyone can") {
                style {
                    opacity = 0.9
                    fontSize = 15.px
                }
            }
            gridpane {
                paddingLeft = 5.0
                val cc = ColumnConstraints(30.0)
                cc.hgrow = Priority.ALWAYS
                columnConstraints.addAll(cc, cc)
                style {
                    fontSize = 12.px
                }
                row {
                    label("Join") {
                        style { opacity = 0.9 }
                    }
                    label(stringBinding(room.guest) {
                        if (value == true) "Yes" else "No"
                    })
                }

                row {
                    label("View") {
                        style { opacity = 0.9 }
                    }
                    label(stringBinding(room.world_read) {
                        if (value == true) "Yes" else "No"
                    })
                }
            }
        }
    }
}
