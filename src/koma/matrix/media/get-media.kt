package service

import koma.storage.config.config_paths
import koma_app.appState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Created by developer on 2017/7/11.
 */

fun getMedia(mxc: String): ByteArray? {
    val parts = mxc.substringAfter("mxc://")
    val server = parts.substringBefore('/')
    val media = parts.substringAfter('/')
    val dir = config_paths.getOrCreate("cache", "media", server, media)
    val file = if (dir != null) File(dir + File.separator + "file.bin") else null
    if(file != null && file.exists()) {
            val instream = FileInputStream(file)
            val buf = instream.readBytes()
            return buf
    }

    val fetched = appState.apiClient?.downloadMedia(mxc)
    if (fetched == null)
        return null
    println("successfully fetched $mxc ${fetched.size}")
    val ostream = FileOutputStream(file)
    ostream.write(fetched)
    ostream.close()
    return fetched
}
