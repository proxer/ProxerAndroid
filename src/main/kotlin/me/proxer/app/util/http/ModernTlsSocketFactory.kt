package me.proxer.app.util.http

import okhttp3.TlsVersion
import timber.log.Timber
import java.net.InetAddress
import java.net.Socket
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Taken and adjusted from here: https://gist.github.com/ankushg/8c0c3144318b1c17abb228d6211ba996
 */
class ModernTlsSocketFactory(trustManager: X509TrustManager) : SSLSocketFactory() {

    private val delegate = try {
        try {
            SSLContext.getInstance(TlsVersion.TLS_1_3.javaName())
        } catch (error: NoSuchAlgorithmException) {
            try {
                SSLContext.getInstance(TlsVersion.TLS_1_2.javaName())
            } catch (error: NoSuchAlgorithmException) {
                try {
                    SSLContext.getInstance(TlsVersion.TLS_1_1.javaName())
                } catch (error: NoSuchAlgorithmException) {
                    SSLContext.getInstance(TlsVersion.TLS_1_0.javaName())
                }
            }
        }
    } catch (error: NoSuchAlgorithmException) {
        Timber.e(error, "Error while trying to load TLS")

        throw error
    }.let {
        it.init(null, arrayOf(trustManager), null)

        it.socketFactory
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return delegate.createSocket(s, host, port, autoClose).patchForModernTls()
    }

    override fun createSocket(host: String, port: Int): Socket? {
        return delegate.createSocket(host, port).patchForModernTls()
    }

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return delegate.createSocket(host, port, localHost, localPort).patchForModernTls()
    }

    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return delegate.createSocket(host, port).patchForModernTls()
    }

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return delegate.createSocket(address, port, localAddress, localPort).patchForModernTls()
    }

    private fun Socket.patchForModernTls(): Socket {
        return (this as? SSLSocket)
            ?.apply {
                enabledProtocols = supportedProtocols
                enabledCipherSuites = supportedCipherSuites

                enabledProtocols = enabledProtocols.toSet()
                    .minus(TlsVersion.SSL_3_0.javaName())
                    .toTypedArray()
            }
            ?: this
    }
}
