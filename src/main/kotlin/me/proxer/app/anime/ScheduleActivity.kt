package me.proxer.app.anime

import android.app.Activity
import android.os.Bundle
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class ScheduleActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) {
            context.startActivity<ScheduleActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, AnimeFragment.newInstance())
                    .commitNow()
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_schedule)
    }
}
