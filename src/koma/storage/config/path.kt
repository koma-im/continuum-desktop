package koma.storage.config

import util.getConfigDir
import java.io.File

object config_paths {
    val config_home = getConfigDir()
    val profile_dir = getOrCreate("profile", create = true)

    fun getCreateProfileDir(vararg paths: String, create: Boolean = true): String? {
        return getOrCreate(*paths, base = profile_dir, create = create)
    }

    fun getOrCreate(vararg paths: String, base: String? = null, create: Boolean = true): String? {
        var curdir = base ?: config_home
        for (p in paths) {
            curdir += File.separator + p
            val dir = File(curdir)
            if (!dir.exists()) {
                if (create) {
                    val result = dir.mkdir()
                    if (!result) {
                        println("failed to create $dir")
                        return null
                    }
                } else
                    return null
            }
        }
        return curdir
    }

    fun getCreateDir(vararg paths: String): File? {
        val ss = listOf(config_home, *paths)
        val path = ss.joinToString(File.separator)
        val file =File(path)
        return if (file.exists()) {
            if (file.isDirectory) file
            else null
        } else if(file.mkdirs()) {
            file
        } else null
    }
}


