package me.proxer.app.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.drawerlayout.widget.DrawerLayout
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils

class CrossfadingDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {

    var crossfader: Crossfader<*>? = null

    private val isTablet = DeviceUtils.isTablet(context)

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isTablet) {
            findViewById<MaterialDrawerSliderView>(R.id.slider)?.let { removeView(it) }
        }
    }

    override fun openDrawer(gravity: Int, animate: Boolean) {
        if (isTablet) {
            crossfader?.crossFade()
        } else {
            super.openDrawer(gravity, animate)
        }
    }

    override fun closeDrawer(gravity: Int, animate: Boolean) {
        if (isTablet) {
            crossfader?.crossFade()
        } else {
            super.closeDrawer(gravity, animate)
        }
    }
}
