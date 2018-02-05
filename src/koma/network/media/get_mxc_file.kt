package koma.network.media

import koma.storage.config.config_paths
import koma_app.appState
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

fun getFileByMxc(mxc: String): File? {
    val parts = mxc.substringAfter("mxc://")
    val server = parts.substringBefore('/')
    val media = parts.substringAfter('/')
    val dir = config_paths.getOrCreate("cache", "media", server)
    val file = if (dir != null) File(dir + File.separator + media) else null
    file ?: return null
    if(file.isFile) {
        return file
    } else {
        file.deleteRecursively()
    }

    val fetched = appState.apiClient?.downloadMedia(mxc)
    fetched?:return null
    val ostream = try {
        FileOutputStream(file)
    } catch (e: FileNotFoundException) {
        return null
    }
    ostream.write(fetched)
    ostream.close()
    return file
}
