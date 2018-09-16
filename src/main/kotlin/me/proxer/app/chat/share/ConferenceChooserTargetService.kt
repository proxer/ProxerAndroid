package me.proxer.app.chat.share

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import androidx.core.os.bundleOf
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.Utils
import me.proxer.library.util.ProxerUrls
import org.koin.android.ext.android.inject
import java.util.Date

/**
 * @author Ruben Gees
 */
@TargetApi(Build.VERSION_CODES.M)
class ConferenceChooserTargetService : ChooserTargetService() {

    companion object {
        const val ARGUMENT_CONFERENCE = "conference"
        const val ARGUMENT_CONFERENCE_WRAPPER = "conference_wrapper" /* Hack for making it possible to share
                                                                        between processes. */
    }

    private val messengerDao by inject<MessengerDao>()

    @TargetApi(Build.VERSION_CODES.M)
    override fun onGetChooserTargets(component: ComponentName, filter: IntentFilter) = messengerDao.getConferences()
        .asSequence()
        .take(8)
        .map {
            val bundle = Bundle()

            bundle.putParcelable(ARGUMENT_CONFERENCE, it)
            bundle.classLoader = LocalConference::class.java.classLoader

            ChooserTarget(
                it.topic,
                constructIcon(it),
                calculateScore(it.date),
                ComponentName(packageName, ShareReceiverActivity::class.java.name),
                bundleOf(ARGUMENT_CONFERENCE_WRAPPER to bundleOf(ARGUMENT_CONFERENCE to it))
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
        .sizeDp(DeviceUtils.getScreenWidth(applicationContext) / 6)
        .paddingDp(DeviceUtils.getScreenWidth(applicationContext) / 32)
        .colorRes(R.color.colorPrimary)
        .let { drawable ->
            when {
                conference.isGroup -> drawable.icon(CommunityMaterial.Icon.cmd_account_multiple)
                else -> drawable.icon(CommunityMaterial.Icon.cmd_account)
            }
        }.toBitmap()

    private fun calculateScore(conferenceDate: Date): Float {
        val score = Date().time.toFloat() / conferenceDate.time.toFloat()

        return score.let { if (it > 1.0f) 1.0f else it }
    }
}
