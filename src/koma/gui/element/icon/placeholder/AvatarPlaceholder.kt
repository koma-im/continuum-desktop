package koma.gui.element.icon.placeholder

import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import koma.graphic.getImageForName
import tornadofx.*

class AvatarPlaceholder private constructor(): ImageView() {
    constructor(name: String, color: Color): this() {
        val im = getImageForName(name, color)
        this.image = im
    }

    constructor(nameV: ObservableValue<String>, color: Color): this() {
        val imv = objectBinding(nameV) { getImageForName(value, color) }
        this.imageProperty().bind(imv)
    }

    constructor(nameV: ObservableValue<String>, color: ObservableValue<Color>): this() {
        nameV.addListener { o: Observable -> redraw(nameV.value, color.value) }
        color.addListener { o: Observable -> redraw(nameV.value, color.value) }
    }

    private fun redraw(name: String?, color: Color?) {
        name ?: return
        color ?: return
        val im = getImageForName(name, color)
        this.image = im
    }
}

