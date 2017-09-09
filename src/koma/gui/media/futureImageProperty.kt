package koma.gui.media

import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.scene.image.Image
import koma.graphic.getResizedImage

fun getMxcImagePropery(mxc: String, width: Double, height: Double): SimpleObjectProperty<Image> {
    val prop = SimpleObjectProperty<Image>(null)
    val task = FetchMxcImageTask(mxc, width, height)
    task.setOnSucceeded {
        val im = task.value
        if (im != null) {
            prop.set(im)
        }
    }
    Thread(task).start()
    return prop
}

private class FetchMxcImageTask(val url: String, val width: Double, val height: Double): Task<Image?>() {
    override fun call(): Image? {
        return getResizedImage(url, width, height)
    }
}
