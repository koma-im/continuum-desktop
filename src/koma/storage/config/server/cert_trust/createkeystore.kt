package koma.storage.config.server.cert_trust

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory

fun createKeyStore(instream: InputStream): KeyStore {
    val cf = CertificateFactory.getInstance("X.509")
    val ca = cf.generateCertificate(instream)
    instream.close()
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    ks.load(null, null)
    ks.setCertificateEntry("ca", ca)
    return ks
}
