package me.proxer.app.anime

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.jakewharton.rxbinding3.view.systemUiVisibilityChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.EPISODE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.NAME_EXTRA
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class StreamActivity : BaseActivity() {

    private val name: String?
        get() = intent.getStringExtra(NAME_EXTRA)

    private val episode: Int?
        get() = intent.getIntExtra(EPISODE_EXTRA, -1).let { if (it <= 0) null else it }

    private val isProxerStream: Boolean
        get() = intent.dataString
            ?.let { Utils.parseAndFixUrl(it) }
            ?.let { ProxerUrls.hasProxerStreamFileHost(it) }
            ?: false

    private val client by inject<OkHttpClient>()
    private val playerManager by unsafeLazy { StreamPlayerManager(this, client) }

    private val root: ViewGroup by bindView(R.id.root)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val playerView: PlayerView by bindView(R.id.player)

    private var mediaRouteButton: MenuItem? = null
    private var introductoryOverlay: IntroductoryOverlay? = null

    private val castStateListener = CastStateListener { newState ->
        if (newState != CastState.NO_DEVICES_AVAILABLE && !storageHelper.wasCastIntroductoryOverlayShown) {
            showIntroductoryOverlay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stream)
        setSupportActionBar(toolbar)
        setupUi()

        playerManager.playerReadySubject
            .autoDisposable(this.scope())
            .subscribe {
                toggleStableControls(it is CastPlayer)

                playerView.player = it
            }

        playerManager.errorSubject
            .autoDisposable(this.scope())
            .subscribe {
                MaterialDialog(this@StreamActivity)
                    .message(it.message)
                    .positiveButton(R.string.error_action_retry) { playerManager.retry() }
                    .negativeButton(R.string.error_action_finish) { finish() }
                    .onCancel { finish() }
                    .show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.activity_stream, menu)

        if (isProxerStream) {
            mediaRouteButton = CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.action_cast)
        }

        return true
    }

    override fun onStart() {
        super.onStart()

        getSafeCastContext()?.addCastStateListener(castStateListener)

        playerManager.start()
    }

    override fun onStop() {
        getSafeCastContext()?.removeCastStateListener(castStateListener)

        playerManager.stop()

        super.onStop()
    }

    override fun onDestroy() {
        introductoryOverlay?.remove()
        introductoryOverlay = null

        playerView.player = null

        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data != null && intent.data != this.intent.data) {
            this.intent = intent

            setupUi()

            playerManager.reset()
        }
    }

    private fun setupUi() {
        title = name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = episode?.let { Category.ANIME.toEpisodeAppString(this, it) }

        playerView.setControllerVisibilityListener {
            toggleFullscreen(it == View.GONE)

            toolbar.isVisible = it == View.VISIBLE
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val newInsets = ViewCompat.onApplyWindowInsets(root, insets)

            if (newInsets.isConsumed) {
                return@setOnApplyWindowInsetsListener newInsets
            }

            ViewCompat.dispatchApplyWindowInsets(toolbar, newInsets)

            if (newInsets.isConsumed) newInsets.consumeSystemWindowInsets() else newInsets
        }

        toggleFullscreen(true)

        window.decorView.systemUiVisibilityChanges()
            .autoDisposable(this.scope())
            .subscribe { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    playerView.showController()
                    toolbar.isVisible = true
                } else {
                    playerView.hideController()
                    toolbar.isVisible = false
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = when {
            fullscreen -> SYSTEM_UI_FLAG_LOW_PROFILE or
                SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_FULLSCREEN
            else -> SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun toggleStableControls(stable: Boolean) {
        if (stable) {
            playerView.controllerHideOnTouch = false
            playerView.controllerShowTimeoutMs = 0
            playerView.showController()
        } else {
            playerView.controllerHideOnTouch = true
            playerView.controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
        }
    }

    private fun showIntroductoryOverlay() {
        val safeIntroductoryOverlay = introductoryOverlay
        val safeMediaRouteButton = mediaRouteButton

        if (safeIntroductoryOverlay != null) {
            safeIntroductoryOverlay.remove()

            introductoryOverlay = null
        } else if (safeMediaRouteButton != null && safeMediaRouteButton.isVisible) {
            toggleStableControls(true)

            Handler().postDelayed(50) {
                storageHelper.wasCastIntroductoryOverlayShown = true

                introductoryOverlay = IntroductoryOverlay.Builder(this, safeMediaRouteButton)
                    .setTitleText(R.string.activity_stream_cast_introduction)
                    .setOnOverlayDismissedListener {
                        toggleStableControls(playerManager.currentPlayer is CastPlayer)

                        introductoryOverlay = null
                    }
                    .build()
                    .apply { show() }
            }
        }
    }

    private fun getSafeCastContext(): CastContext? {
        val availabilityResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        return if (availabilityResult == ConnectionResult.SUCCESS) {
            CastContext.getSharedInstance(this)
        } else {
            null
        }
    }
}
