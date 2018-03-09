package me.proxer.app.notification

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
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
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NotificationFragment.newInstance())
                .commitNow()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_notifications)
    }
}
