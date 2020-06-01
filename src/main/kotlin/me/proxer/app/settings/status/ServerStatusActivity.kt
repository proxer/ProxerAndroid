package me.proxer.app.settings.status

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.commitNow
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ServerStatusActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ServerStatusActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.section_server_status)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ServerStatusFragment.newInstance())
            }
        }
    }
}
