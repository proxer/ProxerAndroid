package me.proxer.app

import cat.ereza.customactivityoncrash.config.CaocConfig

/**
 * @author Ruben Gees
 */
@Suppress("unused")
object FlavorInitializer {

    fun initialize(@Suppress("UNUSED_PARAMETER") application: MainApplication) {
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_CRASH)
            .apply()
    }
}
