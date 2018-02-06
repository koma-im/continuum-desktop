package koma.gui.element.icon

import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.scene.paint.Color
import koma.gui.element.icon.avatar.AvatarView
import koma.gui.element.icon.placeholder.AvatarPlaceholder
import tornadofx.*

class AvatarAlways private constructor(ai: AvatarView, ap: AvatarPlaceholder): Group() {
    constructor(
            urlV: ObservableValue<String>,
            nameV: ObservableValue<String>,
            color: Color
    ): this(
            AvatarView(urlV),
            AvatarPlaceholder(nameV, color))

    constructor(
            urlV: ObservableValue<String>,
            nameV: ObservableValue<String>,
            colorV: ObservableValue<Color>
    ): this(
            AvatarView(urlV),
            AvatarPlaceholder(nameV, colorV))

    init {
        ap.removeWhen { ai.imageAvailable }
        this.add(ap)
        this.add(ai)
    }
}
