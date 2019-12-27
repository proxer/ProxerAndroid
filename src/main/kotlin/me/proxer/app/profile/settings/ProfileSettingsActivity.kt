package me.proxer.app.profile.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commitNow
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class ProfileSettingsActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ProfileSettingsActivity>()
    }

    override val contentView: Int
        get() = R.layout.activity_ucp_settings

    private val viewModel by viewModel<ProfileSettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        viewModel.error.observe(this, Observer {
            it?.let {
                multilineSnackbar(
                    getString(R.string.error_refresh, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage,
                    it.toClickListener(this) ?: View.OnClickListener { viewModel.refresh() }
                )
            }
        })

        viewModel.updateError.observe(this, Observer {
            it?.let {
                multilineSnackbar(
                    getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage,
                    it.toClickListener(this) ?: View.OnClickListener { viewModel.retryUpdate() }
                )
            }
        })

        storageHelper.isLoggedInObservable
            .filter { it.not() }
            .autoDisposable(this.scope())
            .subscribe { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ProfileSettingsFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_profile_settings)
    }
}
