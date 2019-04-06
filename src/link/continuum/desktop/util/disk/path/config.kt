package link.continuum.desktop.util.disk.path

import mu.KotlinLogging
import java.io.File
import java.io.InputStream

private val logger = KotlinLogging.logger {}
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

/**
 * if there is a certificate file at the given path
 * it will be trusted
 */
fun loadOptionalCert(base: File): InputStream? {
    val f = base.resolve("settings").resolve("self-cert.crt")
    if (!f.isFile) return null
    try {
        return  f.inputStream()
    } catch (e: Exception) {
        logger.warn { "Loaded no certificate from ${f.path}: $e" }
    }
    return null
}

