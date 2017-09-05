package service

import koma_app.appState
import util.getCreateAppDataDir
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
    val dir = getCreateAppDataDir("cache", "media", server, media)
    val file = if (dir != null) File(dir + File.separator + "file.bin") else null
    if(file != null && file.exists()) {
            val instream = FileInputStream(file)
            val buf = instream.readBytes()
            println("cached buf $mxc ${buf.size}")
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
