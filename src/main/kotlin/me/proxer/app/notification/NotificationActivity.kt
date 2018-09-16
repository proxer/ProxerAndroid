package me.proxer.app.notification

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.transaction
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

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

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.transaction(now = true) {
                replace(R.id.container, NotificationFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_notifications)
    }
}
