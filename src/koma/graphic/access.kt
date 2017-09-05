package koma.graphic

import javafx.scene.image.Image
import service.getMedia
import java.io.ByteArrayInputStream

/**
 * actual width may be smaller than expected
 */
fun getResizedImage(mxc: String, width: Double, height: Double): Image? {
    val imdata = getMedia(mxc)
    if (imdata == null)
        return null
    val im = Image(ByteArrayInputStream(imdata), width, height, true , width < 120)
    return im
}
