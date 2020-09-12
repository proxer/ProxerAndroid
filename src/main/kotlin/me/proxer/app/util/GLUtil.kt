package me.proxer.app.util

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.max

/**
 * Taken from: https://github.com/inorichi/tachiyomi/blob/master/app/src/main/java/eu/kanade/tachiyomi/util/GLUtil.java
 */
object GLUtil {

    private const val DEFAULT_MAX_BITMAP_DIMENSION = 2_048

    val maxTextureSize by lazy {
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        IntArray(2).also {
            egl.eglInitialize(display, it)
        }

        val totalConfigurations = IntArray(1).also {
            egl.eglGetConfigs(display, null, 0, it)
        }

        val configurationsList = arrayOfNulls<EGLConfig>(totalConfigurations.first()).also {
            egl.eglGetConfigs(display, it, totalConfigurations.first(), totalConfigurations)
        }

        val maximumTextureSize = (0 until totalConfigurations.first())
            .map { configurationPosition ->
                val configuration = configurationsList[configurationPosition]

                IntArray(1).also {
                    egl.eglGetConfigAttrib(display, configuration, EGL10.EGL_MAX_PBUFFER_WIDTH, it)
                }
            }
            .map { it.first() }
            .maxOrNull()

        egl.eglTerminate(display)

        max(maximumTextureSize ?: 0, DEFAULT_MAX_BITMAP_DIMENSION)
    }
}
