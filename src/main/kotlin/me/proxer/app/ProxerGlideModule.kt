package me.proxer.app

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import me.proxer.app.MainApplication.Companion.client
import java.io.InputStream

/**
 * @author Ruben Gees
 */
@GlideModule
class ProxerGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }

    override fun applyOptions(context: Context?, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(RequestOptions().disallowHardwareConfig())
    }

    override fun isManifestParsingEnabled() = false
}
