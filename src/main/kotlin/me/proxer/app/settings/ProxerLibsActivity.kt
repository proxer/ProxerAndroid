package me.proxer.app.settings

import android.os.Bundle
import androidx.fragment.app.commitNow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity

class ProxerLibsActivity : DrawerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = intent.extras?.getString(Libs.BUNDLE_TITLE, "")

        val fragment = LibsSupportFragment().apply {
            arguments = intent.extras
        }

        supportFragmentManager.commitNow {
            replace(R.id.container, fragment)
        }
    }
}
