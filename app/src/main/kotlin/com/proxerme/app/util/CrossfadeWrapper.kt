package com.proxerme.app.util

import com.mikepenz.crossfader.Crossfader
import com.mikepenz.materialdrawer.interfaces.ICrossfader

class CrossfadeWrapper(private val crossfader: Crossfader<*>): ICrossfader {

    override fun crossfade() {
        crossfader.crossFade()
    }

    override fun isCrossfaded(): Boolean = crossfader.isCrossFaded()
}