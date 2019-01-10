package link.continuum.desktop.util.disk.path

import java.io.File

private const val app_name = "continuum"

fun getConfigDir(): String {
    val env = System.getenv()
    val config_home: String = env.get("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + File.separator + ".config")
    val config_dir = config_home + File.separator + app_name
    val dir: File = File(config_dir)
    if (!dir.isDirectory()) {
        dir.mkdir()
    }
    return config_dir
}

