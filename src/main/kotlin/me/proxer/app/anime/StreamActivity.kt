package me.proxer.app.anime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.systemUiVisibilityChanges
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.colorRes
import com.mikepenz.iconics.paddingDp
import com.mikepenz.iconics.sizeDp
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.mikepenz.iconics.utils.toIconicsColorRes
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.SINGLE_CONNECTION_CLIENT
import me.proxer.app.anime.StreamPlayerManager.PlayerState
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.AD_TAG_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.EPISODE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.INTERNAL_PLAYER_ONLY_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.NAME_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.REFERER_EXTRA
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.newTask
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import me.zhanghai.android.materialprogressbar.ThinCircularProgressDrawable
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

/**
 * @author Ruben Gees
 */
class StreamActivity : BaseActivity() {

    internal val name: String?
        get() = intent.getStringExtra(NAME_EXTRA)

    internal val episode: Int?
        get() = intent.getIntExtra(EPISODE_EXTRA, -1).let { if (it <= 0) null else it }

    internal val referer: String?
        get() = intent.getStringExtra(REFERER_EXTRA)

    internal val uri: Uri
        get() = requireNotNull(intent.data)

    private val mimeType: String
        get() = requireNotNull(intent.type)

    private val isInternalPlayerOnly: Boolean
        get() = intent.getBooleanExtra(INTERNAL_PLAYER_ONLY_EXTRA, false)

    private val isProxerStream: Boolean
        get() = intent.dataString
            ?.let { Utils.parseAndFixUrl(it) }
            ?.let { ProxerUrls.hasProxerStreamFileHost(it) }
            ?: false

    private val adTag: Uri?
        get() = intent.getParcelableExtra(AD_TAG_EXTRA)

    private val client by inject<OkHttpClient>(named(SINGLE_CONNECTION_CLIENT))
    private val playerManager by unsafeLazy { StreamPlayerManager(this, client, adTag) }

    internal val playerView: TouchablePlayerView by bindView(R.id.player)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    private val loading: ProgressBar by bindView(R.id.loading)
    private val rewindIndicator: TextView by bindView(R.id.rewindIndicator)
    private val fastForwardIndicator: TextView by bindView(R.id.fastForwardIndicator)

    private val play: ImageButton by bindView(R.id.play)
    private val fullscreen: ImageButton by bindView(R.id.fullscreen)
    private val controlIcon: ImageView by bindView(R.id.controlIcon)
    private val controlProgress: MaterialProgressBar by bindView(R.id.controlProgress)
    private val controlsContainer: ViewGroup by bindView(R.id.controlsContainer)

    private var mediaRouteButton: MenuItem? = null
    private var introductoryOverlay: IntroductoryOverlay? = null

    private val castStateListener = CastStateListener { newState ->
        if (newState != CastState.NO_DEVICES_AVAILABLE && !storageHelper.wasCastIntroductoryOverlayShown) {
            showIntroductoryOverlay()
        }
    }

    private val animationTime by unsafeLazy { resources.getInteger(android.R.integer.config_shortAnimTime).toLong() }

    private val hideIndicatorHandler = Handler()
    private val adFullscreenHandler = Handler()
    private val hideControlHandler = Handler()
    private val animationHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stream)
        setSupportActionBar(toolbar)
        setupUi()

        fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon.cmd_fullscreen))
        controlProgress.progressDrawable = ThinCircularProgressDrawable(this)
        controlProgress.showProgressBackground = false

        rewindIndicator.setCompoundDrawables(
            null, generateIndicatorIcon(CommunityMaterial.Icon2.cmd_rewind), null, null
        )

        fastForwardIndicator.setCompoundDrawables(
            null, generateIndicatorIcon(CommunityMaterial.Icon.cmd_fast_forward), null, null
        )

        playerManager.playerReadySubject
            .autoDisposable(this.scope())
            .subscribe {
                toggleStableControls(it is CastPlayer)

                playerView.player = it
            }

        playerManager.playerStateSubject
            .autoDisposable(this.scope())
            .subscribe {
                when (it) {
                    PlayerState.PLAYING -> {
                        play.contentDescription = getString(R.string.exoplayer_pause_description)
                        play.setImageState(intArrayOf(R.attr.state_pause), true)

                        loading.isVisible = false
                        play.isVisible = true
                    }
                    PlayerState.PAUSING -> {
                        play.contentDescription = getString(R.string.exoplayer_play_description)
                        play.setImageState(intArrayOf(-R.attr.state_pause), true)

                        loading.isVisible = false
                        play.isVisible = true
                    }
                    PlayerState.LOADING -> {
                        loading.isVisible = true
                        play.isVisible = false
                    }
                    null -> error("playerState is null")
                }

                if ((controlsContainer.parent as? View)?.isVisible?.not() != false) {
                    play.jumpDrawablesToCurrentState()
                }
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

        playerView.rewindSubject
            .autoDisposable(this.scope())
            .subscribe {
                resetIndicator(fastForwardIndicator)
                updateIndicator(rewindIndicator)
            }

        playerView.fastForwardSubject
            .autoDisposable(this.scope())
            .subscribe {
                resetIndicator(rewindIndicator)
                updateIndicator(fastForwardIndicator)
            }

        playerView.volumeChangeSubject
            .autoDisposable(this.scope())
            .subscribe {
                val icon = when {
                    it <= 0 -> CommunityMaterial.Icon2.cmd_volume_mute
                    it <= 33 -> CommunityMaterial.Icon2.cmd_volume_low
                    it <= 66 -> CommunityMaterial.Icon2.cmd_volume_medium
                    else -> CommunityMaterial.Icon2.cmd_volume_high
                }

                updateControl(it, icon)
            }

        playerView.brightnessChangeSubject
            .autoDisposable(this.scope())
            .subscribe {
                val icon = when {
                    it <= 33 -> CommunityMaterial.Icon.cmd_brightness_5
                    it <= 66 -> CommunityMaterial.Icon.cmd_brightness_6
                    else -> CommunityMaterial.Icon.cmd_brightness_7
                }

                updateControl(it, icon)
            }

        play.clicks()
            .autoDisposable(this.scope())
            .subscribe { playerManager.toggle() }

        rewindIndicator.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                playerView.rewind()

                updateIndicator(rewindIndicator, animate = false)
            }

        fastForwardIndicator.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                playerView.fastForward()

                updateIndicator(fastForwardIndicator, animate = false)
            }

        fullscreen.clicks()
            .autoDisposable(this.scope())
            .subscribe { toggleOrientation() }

        if (savedInstanceState == null) {
            toggleOrientation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_stream, menu, true)

        if (isProxerStream) {
            mediaRouteButton = CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.action_cast)
        }

        if (isInternalPlayerOnly || !canOpenInOtherApp()) {
            menu.findItem(R.id.action_open_in_other_app).isVisible = false
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val indicatorMargin = resources.getDimensionPixelSize(R.dimen.stream_indicator_margin)

        rewindIndicator.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart = indicatorMargin }
        fastForwardIndicator.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginEnd = indicatorMargin }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_in_other_app -> openInOtherApp()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        getSafeCastContext()?.addCastStateListener(castStateListener)

        playerManager.play()
    }

    override fun onStop() {
        getSafeCastContext()?.removeCastStateListener(castStateListener)

        playerManager.pause()

        super.onStop()
    }

    override fun onDestroy() {
        introductoryOverlay?.remove()
        introductoryOverlay = null

        playerView.player = null

        hideIndicatorHandler.removeCallbacksAndMessages(null)
        adFullscreenHandler.removeCallbacksAndMessages(null)
        hideControlHandler.removeCallbacksAndMessages(null)
        animationHandler.removeCallbacksAndMessages(null)

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

    @Suppress("SwallowedException")
    internal fun getSafeCastContext(): CastContext? {
        val availabilityResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        return if (availabilityResult == ConnectionResult.SUCCESS) {
            try {
                CastContext.getSharedInstance(this)
            } catch (_: Exception) {
                null
            }
        } else {
            null
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

        window.decorView.systemUiVisibilityChanges()
            .autoDisposable(this.scope())
            .subscribe { visibility ->
                if (playerManager.isPlayingAd.not()) {
                    if (visibility and SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        playerView.showController()
                        toolbar.isVisible = true
                    } else {
                        playerView.hideController()
                        toolbar.isVisible = false
                    }
                } else {
                    adFullscreenHandler.postDelayed(3_000) {
                        toggleFullscreen(true)
                    }
                }
            }

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        toggleFullscreen(true)
    }

    private fun toggleOrientation() {
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon.cmd_fullscreen))
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon.cmd_fullscreen_exit))
        }
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = when {
            fullscreen -> SYSTEM_UI_FLAG_LOW_PROFILE or
                SYSTEM_UI_FLAG_FULLSCREEN or
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
            playerView.controllerShowTimeoutMs = 2_000
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

    private fun resetIndicator(view: TextView) {
        view.background.state = intArrayOf()
        view.isVisible = false
        view.text = ""

        view.jumpDrawablesToCurrentState()

        hideIndicatorHandler.removeCallbacksAndMessages(null)
        animationHandler.removeCallbacksAndMessages(null)
    }

    private fun updateIndicator(view: TextView, animate: Boolean = true) {
        val previousDuration = view.text.toString().toIntOrNull()
        val wasVisible = view.isVisible

        view.isVisible = true
        view.text = ((previousDuration ?: 0) + 10).toString()

        if (animate) {
            view.background.state = intArrayOf(
                android.R.attr.state_pressed, android.R.attr.state_enabled
            )

            animationHandler.postDelayed(animationTime) {
                view.background.state = intArrayOf()
            }
        }

        if (wasVisible) {
            hideIndicatorHandler.removeCallbacksAndMessages(null)
        }

        hideIndicatorHandler.postDelayed(1_000) {
            resetIndicator(view)
        }
    }

    private fun updateControl(value: Int, icon: IIcon) {
        hideControlHandler.removeCallbacksAndMessages(null)

        controlProgress.isVisible = true
        controlIcon.isVisible = true

        controlProgress.progress = value
        controlIcon.setImageDrawable(
            IconicsDrawable(this, icon)
                .sizeDp(64)
                .paddingDp(12)
                .colorRes(android.R.color.white)
        )

        hideControlHandler.postDelayed(1_000) {
            controlProgress.isVisible = false
            controlIcon.isVisible = false
        }
    }

    private fun generateIndicatorIcon(icon: IIcon) = IconicsDrawable(this, icon)
        .sizeDp(36)
        .paddingDp(4)
        .colorRes(android.R.color.white)

    private fun canOpenInOtherApp(): Boolean {
        val intent = StreamResolutionResult.Video(HttpUrl.get(uri.toString()), mimeType, referer).makeIntent(this)

        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    private fun openInOtherApp(): Boolean {
        return try {
            val intent = StreamResolutionResult.Video(HttpUrl.get(uri.toString()), mimeType, referer)
                .makeIntent(this)
                .newTask()

            startActivity(intent)
            finish()

            true
        } catch (ignored: ActivityNotFoundException) {
            false
        }
    }

    private fun generateControllerIcon(icon: IIcon) = IconicsDrawable(this, icon)
        .size(44.toIconicsSizeDp())
        .padding(8.toIconicsSizeDp())
        .color(android.R.color.white.toIconicsColorRes())
}
