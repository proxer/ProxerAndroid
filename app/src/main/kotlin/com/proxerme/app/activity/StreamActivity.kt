package com.proxerme.app.activity

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import butterknife.bindView
import com.afollestad.easyvideoplayer.EasyVideoCallback
import com.afollestad.easyvideoplayer.EasyVideoPlayer
import com.proxerme.app.R
import com.proxerme.app.util.telephonyManager


class StreamActivity : AppCompatActivity() {

    private val player: EasyVideoPlayer by bindView(R.id.player)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        player.setSource(intent.data)
        player.setAutoPlay(true)
        player.setAutoFullscreen(true)
        player.setCallback(object : EasyVideoCallback {
            override fun onPrepared(player: EasyVideoPlayer?) {
            }

            override fun onStarted(player: EasyVideoPlayer?) {
            }

            override fun onCompletion(player: EasyVideoPlayer?) {
            }

            override fun onRetry(player: EasyVideoPlayer?, source: Uri?) {
            }

            override fun onSubmit(player: EasyVideoPlayer?, source: Uri?) {
            }

            override fun onBuffering(percent: Int) {
            }

            override fun onPreparing(player: EasyVideoPlayer?) {
            }

            override fun onError(player: EasyVideoPlayer?, e: Exception?) {
            }

            override fun onPaused(player: EasyVideoPlayer?) {
            }
        })

        telephonyManager.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING ->
                        player.pause()
                    TelephonyManager.CALL_STATE_IDLE ->
                        player.start()
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    public override fun onPause() {
        player.pause()

        super.onPause()
    }

    override fun onDestroy() {
        player.release()

        super.onDestroy()
    }
}
