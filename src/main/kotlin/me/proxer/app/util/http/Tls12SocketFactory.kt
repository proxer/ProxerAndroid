package me.proxer.app.util.http

import okhttp3.TlsVersion
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Taken and adjusted from here: https://gist.github.com/ankushg/8c0c3144318b1c17abb228d6211ba996
 */
class Tls12SocketFactory(trustManager: X509TrustManager) : SSLSocketFactory() {

    private val delegate = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName()).let {
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
        return delegate.createSocket(s, host, port, autoClose)
            .patchForTls12()
    }

    override fun createSocket(host: String, port: Int): Socket? {
        return delegate.createSocket(host, port)
            .patchForTls12()
    }

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
        return delegate.createSocket(host, port, localHost, localPort)
            .patchForTls12()
    }

    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return delegate.createSocket(host, port)
            .patchForTls12()
    }

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
        return delegate.createSocket(address, port, localAddress, localPort)
            .patchForTls12()
    }

    private fun Socket.patchForTls12(): Socket {
        return (this as? SSLSocket)?.apply {
            enabledProtocols += TlsVersion.TLS_1_2.javaName()
        } ?: this
    }
}
