package me.proxer.app.notification

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.commitNow
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class NotificationActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<NotificationActivity>()
        fun getIntent(context: Context) = context.intentFor<NotificationActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.section_notifications)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, NotificationFragment.newInstance())
            }
        }
    }
}
