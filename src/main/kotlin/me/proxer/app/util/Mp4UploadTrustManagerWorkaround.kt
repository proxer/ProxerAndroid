package me.proxer.app.util

import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.internal.platform.Platform
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

object Mp4UploadTrustManagerWorkaround {

    fun create(): X509TrustManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createSdk24()
        } else {
            createSdk21()
        }
    }

    private fun createSdk21(): X509TrustManager {
        return object : X509TrustManager {
            private val platformTrustManager = Platform.get().platformTrustManager()

            override fun getAcceptedIssuers(): Array<X509Certificate> = platformTrustManager.acceptedIssuers

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
                platformTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
                checkServerTrustedWithMp4Upload(chain) {
                    platformTrustManager.checkServerTrusted(chain, authType)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun createSdk24(): X509ExtendedTrustManager {
        return object : X509ExtendedTrustManager() {
            private val platformTrustManager = Platform.get().platformTrustManager() as X509ExtendedTrustManager

            override fun getAcceptedIssuers(): Array<X509Certificate> = platformTrustManager.acceptedIssuers

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
                platformTrustManager.checkClientTrusted(chain, authType, socket)
            }

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
                platformTrustManager.checkClientTrusted(chain, authType, engine)
            }

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
                platformTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
                checkServerTrustedWithMp4Upload(chain) {
                    platformTrustManager.checkServerTrusted(chain, authType, socket)
                }
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
                checkServerTrustedWithMp4Upload(chain) {
                    platformTrustManager.checkServerTrusted(chain, authType, engine)
                }
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
                checkServerTrustedWithMp4Upload(chain) {
                    platformTrustManager.checkServerTrusted(chain, authType)
                }
            }
        }
    }

    private fun checkServerTrustedWithMp4Upload(chain: Array<out X509Certificate>, delegate: () -> Unit) {
        val mp4UploadCert = chain.find { it.subjectDN.name == "CN=*.mp4upload.com" }

        if (mp4UploadCert != null) {
            // The certificate chain of MP4Upload contains an expired cross-signed certificate which incorrectly
            // leads to an error on various Android Versions.
            // Avoid checking the entire chain here to workaround that.
            //
            // THIS IS A SECURITY ISSUE AND NEEDS TO BE REMOVED AS SOON AS POSSIBLE.
            mp4UploadCert.checkValidity()
        } else {
            delegate()
        }
    }
}
