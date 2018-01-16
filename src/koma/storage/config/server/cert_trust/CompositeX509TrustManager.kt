package koma.storage.config.server.cert_trust

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Represents an ordered list of [X509TrustManager]s with additive trust. If any one of the composed managers
 * trusts a certificate chain, then it is trusted by the composite manager.
 *
 * This is necessary because of the fine-print on [SSLContext.init]: Only the first instance of a particular key
 * and/or trust manager implementation type in the array is used. (For example, only the first
 * javax.net.ssl.X509KeyManager in the array will be used.)
 *
 * @author codyaray
 * @since 4/22/2013
 * @see [
 * http://stackoverflow.com/questions/1793979/registering-multiple-keystores-in-jvm
](http://stackoverflow.com/questions/1793979/registering-multiple-keystores-in-jvm) *
 */
class CompositeX509TrustManager(keystore: KeyStore) : X509TrustManager {

    private val trustManagers: List<X509TrustManager>

    init {
        this.trustManagers = listOf(defaultTrustManager!!, getTrustManager(keystore)!!)
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType)
                return  // someone trusts them. success!
            } catch (e: CertificateException) {
                // maybe someone else will trust them
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType)
                return  // someone trusts them. success!
            } catch (e: CertificateException) {
                // maybe someone else will trust them
            }

        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        for (trustManager in trustManagers) {
            for (cert in trustManager.acceptedIssuers) {
                certificates.add(cert)
            }
        }
        return certificates.toTypedArray()
    }

    companion object {

        val defaultTrustManager: X509TrustManager?
            get() = getTrustManager(null)

        fun getTrustManager(keystore: KeyStore?): X509TrustManager? {

            return getTrustManager(TrustManagerFactory.getDefaultAlgorithm(), keystore)

        }

        fun getTrustManager(algorithm: String, keystore: KeyStore?): X509TrustManager? {

            val factory: TrustManagerFactory

            try {
                factory = TrustManagerFactory.getInstance(algorithm)
                factory.init(keystore)
                val first = factory.trustManagers.filter { it is X509TrustManager }.firstOrNull()
                return first as? X509TrustManager
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyStoreException) {
                e.printStackTrace()
            }

            return null

        }
    }

}
