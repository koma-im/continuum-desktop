package koma.gui.element.icon

import javafx.beans.value.ObservableValue
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.gui.element.icon.avatar.AvatarView
import koma.gui.element.icon.placeholder.AvatarPlaceholder
import koma.storage.config.settings.AppSettings
import tornadofx.*


val avatarSize = AppSettings.scaling * 32.0

class AvatarAlways private constructor(ai: AvatarView, ap: AvatarPlaceholder): StackPane() {
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

        this.minHeight = avatarSize
        this.minWidth = avatarSize
    }
}
