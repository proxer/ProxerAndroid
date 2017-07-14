package me.proxer.app.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.fragment.ucp.NotificationFragment
import me.proxer.app.util.extension.bindView
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

class NotificationActivity : MainActivity() {

    companion object {
        fun navigateTo(context: Activity) {
            context.startActivity<NotificationActivity>()
        }

        fun getIntent(context: Context): Intent {
            return context.intentFor<NotificationActivity>()
        }
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_notifications)

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
}