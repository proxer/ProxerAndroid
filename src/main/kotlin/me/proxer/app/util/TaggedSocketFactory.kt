package me.proxer.app.util

import androidx.core.net.TrafficStatsCompat
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * @author Ruben Gees
 */
class TaggedSocketFactory : SocketFactory() {

    private val delegate = SocketFactory.getDefault()

    override fun createSocket(): Socket {
        return delegate.createSocket()
            .also { _ -> TrafficStatsCompat.setThreadStatsTag(1) }
    }

    override fun createSocket(host: String?, port: Int): Socket {
        return delegate.createSocket(host, port)
            .also { _ -> TrafficStatsCompat.setThreadStatsTag(1) }
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int
    ): Socket {
        return delegate.createSocket(host, port, localHost, localPort)
            .also { _ -> TrafficStatsCompat.setThreadStatsTag(1) }
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        return delegate.createSocket(host, port)
            .also { _ -> TrafficStatsCompat.setThreadStatsTag(1) }
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket {
        return delegate.createSocket(address, port, localAddress, localPort)
            .also { _ -> TrafficStatsCompat.setThreadStatsTag(1) }
    }
}
