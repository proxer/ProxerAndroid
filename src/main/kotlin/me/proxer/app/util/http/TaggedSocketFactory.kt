package me.proxer.app.util.http

import android.net.TrafficStats
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * @author Ruben Gees
 */
class TaggedSocketFactory : SocketFactory() {

    private val delegate = getDefault()

    override fun createSocket(): Socket {
        return delegate.createSocket()
            .also { TrafficStats.setThreadStatsTag(1) }
    }

    override fun createSocket(host: String?, port: Int): Socket {
        return delegate.createSocket(host, port)
            .also { TrafficStats.setThreadStatsTag(1) }
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int
    ): Socket {
        return delegate.createSocket(host, port, localHost, localPort)
            .also { TrafficStats.setThreadStatsTag(1) }
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        return delegate.createSocket(host, port)
            .also { TrafficStats.setThreadStatsTag(1) }
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket {
        return delegate.createSocket(address, port, localAddress, localPort)
            .also { TrafficStats.setThreadStatsTag(1) }
    }
}
