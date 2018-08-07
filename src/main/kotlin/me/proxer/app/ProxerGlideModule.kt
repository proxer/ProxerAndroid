package me.proxer.app

import android.content.Context
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
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.util.data.PreferenceHelper
import java.io.InputStream

/**
 * @author Ruben Gees
 */
@GlideModule
@Excludes(OkHttpLibraryGlideModule::class)
class ProxerGlideModule : AppGlideModule() {

    private companion object {
        private const val CACHE_SIZE = 1024L * 1024L * 250L
        private const val CACHE_DIR = "cache"
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val externalFilesDir = context.getExternalFilesDir(null)

        builder.setDefaultRequestOptions(RequestOptions()
            .disallowHardwareConfig()
            .format(DecodeFormat.PREFER_RGB_565)
        )

        if (PreferenceHelper.shouldCacheExternally(context) && externalFilesDir != null) {
            builder.setDiskCache(DiskLruCacheFactory(externalFilesDir.path, CACHE_DIR, CACHE_SIZE))
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }

    override fun isManifestParsingEnabled() = false
}
