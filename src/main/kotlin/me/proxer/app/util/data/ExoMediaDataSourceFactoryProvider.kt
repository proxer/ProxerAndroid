package me.proxer.app.util.data

import com.devbrackets.android.exomedia.ExoMedia
import com.gojuno.koptional.Optional
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import okhttp3.OkHttpClient

/**
 * @author Ruben Gees
 */
class ExoMediaDataSourceFactoryProvider(
    client: OkHttpClient,
    referer: Optional<String>
) : ExoMedia.DataSourceFactoryProvider {

    private val exoMediaClient: OkHttpClient = when (
        val nullableReferer = referer.toNullable()) {
        null -> client
        else -> client.newBuilder()
            .addInterceptor { chain ->
                val newRequest =
                    chain.request().newBuilder().header("Referer", nullableReferer).build()

                chain.proceed(newRequest)
            }
            .build()
    }

    override fun provide(
        userAgent: String,
        listener: TransferListener<in DataSource>?
    ): DataSource.Factory {
        return OkHttpDataSourceFactory(exoMediaClient, GENERIC_USER_AGENT, listener)
    }
}
