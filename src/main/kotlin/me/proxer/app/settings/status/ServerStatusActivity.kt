package me.proxer.app.settings.status

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commitNow
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ServerStatusActivity : BaseActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ServerStatusActivity>()
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)

        setSupportActionBar(toolbar)
        setTitle(R.string.section_server_status)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ServerStatusFragment.newInstance())
            }
        }
    }
}
