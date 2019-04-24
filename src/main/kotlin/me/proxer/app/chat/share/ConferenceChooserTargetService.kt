package me.proxer.app.chat.share

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import androidx.core.os.bundleOf
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsColorRes
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.squareup.moshi.Moshi
import me.proxer.app.R
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.Utils
import me.proxer.library.util.ProxerUrls
import org.koin.android.ext.android.inject
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@TargetApi(Build.VERSION_CODES.M)
class ConferenceChooserTargetService : ChooserTargetService() {

    companion object {
        const val ARGUMENT_CONFERENCE = "conference"
    }

    private val messengerDao by inject<MessengerDao>()
    private val moshi by inject<Moshi>()

    @TargetApi(Build.VERSION_CODES.M)
    override fun onGetChooserTargets(component: ComponentName, filter: IntentFilter) = messengerDao.getConferences()
        .asSequence()
        .take(8)
        .map {
            val serializedConference = moshi.adapter(LocalConference::class.java).toJson(it)

            ChooserTarget(
                it.topic,
                constructIcon(it),
                calculateScore(it.date),
                ComponentName(packageName, ShareReceiverActivity::class.java.name),
                bundleOf(ARGUMENT_CONFERENCE to serializedConference)
            )
        }
        .toList()

    @TargetApi(Build.VERSION_CODES.M)
    private fun constructIcon(conference: LocalConference) = when {
        conference.image.isBlank() -> Icon.createWithBitmap(constructEmptyIcon(conference))
        else -> Icon.createWithBitmap(
            Utils.getCircleBitmapFromUrl(
                applicationContext,
                ProxerUrls.userImage(conference.image)
            )
        )
    }

    private fun constructEmptyIcon(conference: LocalConference) = IconicsDrawable(applicationContext)
        .size((DeviceUtils.getScreenWidth(applicationContext) / 6).toIconicsSizeDp())
        .padding((DeviceUtils.getScreenWidth(applicationContext) / 32).toIconicsSizeDp())
        .color(R.color.primary.toIconicsColorRes())
        .let { drawable ->
            when {
                conference.isGroup -> drawable.icon(CommunityMaterial.Icon.cmd_account_multiple)
                else -> drawable.icon(CommunityMaterial.Icon.cmd_account)
            }
        }.toBitmap()

    private fun calculateScore(conferenceDate: Instant): Float {
        val score = Instant.now().toEpochMilli().toFloat() / conferenceDate.toEpochMilli().toFloat()

        return score.let { if (it > 1.0f) 1.0f else it }
    }
}
