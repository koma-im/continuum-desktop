package koma.storage.config.server.cert_trust

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.net.ssl.SSLContext

val certFileName = "self-cert.crt"

fun loadContext(dir: File): Pair<SSLContext, CompositeX509TrustManager>? {
    val sf = dir.resolve(certFileName)
    if (!sf.isFile) return null
    val certStream = try {
        sf.inputStream()
    } catch (e: FileNotFoundException) {
        return null
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    val ks = createKeyStore(certStream)
    val tm = CompositeX509TrustManager(ks)
    val sc = SSLContext.getInstance("TLS")
    sc.init(null, arrayOf(tm), null)
    return  Pair(sc, tm)
}
