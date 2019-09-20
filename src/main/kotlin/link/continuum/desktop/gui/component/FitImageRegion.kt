package link.continuum.desktop.gui.component

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import javafx.scene.layout.*


class FitImageRegion: Region(){
    val imageProperty = SimpleObjectProperty<Image?>()
    var image
        get() = imageProperty.get()
        set(value) {imageProperty.set(value)}
    init {
        imageProperty.addListener { _, _, image ->
            if (image == null) {
                backgroundProperty().set(null)
                return@addListener
            }
            this.backgroundProperty().set(
                    Background(BackgroundImage(image,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            bgSize
                    ))
            )
        }
    }
    companion object {
        private val bgSize =  BackgroundSize(100.0, 100.0,
                true, true,
                false, true)
    }
}