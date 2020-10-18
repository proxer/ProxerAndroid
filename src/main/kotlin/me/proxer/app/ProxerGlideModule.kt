package me.proxer.app

import android.content.Context
import android.os.Environment
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.safeInject
import okhttp3.OkHttpClient
import java.io.InputStream

/**
 * @author Ruben Gees
 */
@GlideModule
@Excludes(OkHttpLibraryGlideModule::class)
class ProxerGlideModule : AppGlideModule() {

    private companion object {
        private const val CACHE_SIZE = 1_024L * 1_024L * 250L
        private const val CACHE_DIR = "glide"
    }

    private val client by safeInject<OkHttpClient>()
    private val preferenceHelper by safeInject<PreferenceHelper>()

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val cacheDir = when (!Environment.isExternalStorageEmulated() && preferenceHelper.shouldCacheExternally) {
            true -> context.externalCacheDir ?: context.cacheDir
            false -> context.cacheDir
        }

        builder
            .setLogRequestOrigins(BuildConfig.DEBUG)
            .setDefaultRequestOptions(
                RequestOptions()
                    .disallowHardwareConfig()
                    .format(DecodeFormat.PREFER_RGB_565)
            )

        builder.setDiskCache(DiskLruCacheFactory(cacheDir.path, CACHE_DIR, CACHE_SIZE))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }

    override fun isManifestParsingEnabled() = false
}
