package me.proxer.app.notification

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class NotificationActivity : BaseActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<NotificationActivity>()
        fun getIntent(context: Context) = context.intentFor<NotificationActivity>()
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

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
