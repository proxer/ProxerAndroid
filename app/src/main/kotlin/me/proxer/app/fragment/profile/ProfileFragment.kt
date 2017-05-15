package me.proxer.app.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.UserInfo
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class ProfileFragment : LoadingFragment<ProxerCall<UserInfo>, UserInfo>() {

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val profileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = profileActivity.userId
    private val username: String?
        get() = profileActivity.username

    private val animePointsRow: TextView by bindView(R.id.animePointsRow)
    private val mangaPointsRow: TextView by bindView(R.id.mangaPointsRow)
    private val uploadPointsRow: TextView by bindView(R.id.uploadPointsRow)
    private val forumPointsRow: TextView by bindView(R.id.forumPointsRow)
    private val infoPointsRow: TextView by bindView(R.id.infoPointsRow)
    private val miscellaneousPointsRow: TextView by bindView(R.id.miscellaneousPointsRow)
    private val totalPointsRow: TextView by bindView(R.id.totalPointsRow)
    private val rank: TextView by bindView(R.id.rank)

    private val statusContainer: ViewGroup by bindView(R.id.statusContainer)
    private val statusText: TextView by bindView(R.id.statusText)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSuccess(result: UserInfo) {
        profileActivity.userId = result.id
        profileActivity.username = result.username
        profileActivity.image = result.image

        val totalPoints = result.animePoints + result.mangaPoints + result.uploadPoints + result.forumPoints +
                result.infoPoints + result.miscPoints

        animePointsRow.text = result.animePoints.toString()
        mangaPointsRow.text = result.mangaPoints.toString()
        uploadPointsRow.text = result.uploadPoints.toString()
        forumPointsRow.text = result.forumPoints.toString()
        infoPointsRow.text = result.infoPoints.toString()
        miscellaneousPointsRow.text = result.miscPoints.toString()
        totalPointsRow.text = totalPoints.toString()
        rank.text = rankToString(totalPoints)

        if (result.status.isBlank()) {
            statusContainer.visibility = View.GONE
        } else {
            val rawText = result.status + " - " + TimeUtils.convertToRelativeReadableTime(context,
                    result.lastStatusChange)

            statusText.text = Utils.buildClickableText(statusText.context, rawText,
                    onWebClickListener = Link.OnClickListener {
                        HttpUrl.parse(it)?.let { showPage(it) }
                    })
        }

        super.onSuccess(result)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<UserInfo>().build()
    override fun constructInput() = api.user().info(userId, username).build()

    private fun rankToString(points: Int): String {
        return context.getString(when {
            (points < 10) -> R.string.rank_10
            (points < 100) -> R.string.rank_100
            (points < 200) -> R.string.rank_200
            (points < 500) -> R.string.rank_500
            (points < 700) -> R.string.rank_700
            (points < 1000) -> R.string.rank_1000
            (points < 1500) -> R.string.rank_1500
            (points < 2000) -> R.string.rank_2000
            (points < 3000) -> R.string.rank_3000
            (points < 4000) -> R.string.rank_4000
            (points < 6000) -> R.string.rank_6000
            (points < 8000) -> R.string.rank_8000
            (points < 10000) -> R.string.rank_10000
            (points < 11000) -> R.string.rank_11000
            (points < 12000) -> R.string.rank_12000
            (points < 14000) -> R.string.rank_14000
            (points < 16000) -> R.string.rank_16000
            (points < 18000) -> R.string.rank_18000
            (points < 20000) -> R.string.rank_20000
            (points > 20000) -> R.string.rank_kami_sama
            else -> throw IllegalArgumentException("Illegal rank: $points")
        })
    }
}
