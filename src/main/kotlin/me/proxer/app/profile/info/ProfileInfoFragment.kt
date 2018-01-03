package me.proxer.app.profile.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.user.UserInfo
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class ProfileInfoFragment : BaseContentFragment<UserInfo>() {

    companion object {
        fun newInstance() = ProfileInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    override val viewModel by unsafeLazy { ProfileInfoViewModelProvider.get(this, userId, username) }

    private val userId: String?
        get() = hostingActivity.userId

    private val username: String?
        get() = hostingActivity.username

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun showData(data: UserInfo) {
        super.showData(data)

        hostingActivity.userId = data.id
        hostingActivity.username = data.username
        hostingActivity.image = data.image

        val totalPoints = data.animePoints + data.mangaPoints + data.uploadPoints + data.forumPoints +
                data.infoPoints + data.miscPoints

        animePointsRow.text = data.animePoints.toString()
        mangaPointsRow.text = data.mangaPoints.toString()
        uploadPointsRow.text = data.uploadPoints.toString()
        forumPointsRow.text = data.forumPoints.toString()
        infoPointsRow.text = data.infoPoints.toString()
        miscellaneousPointsRow.text = data.miscPoints.toString()
        totalPointsRow.text = totalPoints.toString()
        rank.text = rankToString(totalPoints)

        if (data.status.isBlank()) {
            statusContainer.visibility = View.GONE
        } else {
            val rawText = data.status + " - " + data.lastStatusChange.convertToRelativeReadableTime(safeContext)

            statusText.text = Utils.buildClickableText(statusText.context, rawText,
                    onWebClickListener = Link.OnClickListener {
                        showPage(Utils.parseAndFixUrl(it))
                    })
        }
    }

    private fun rankToString(points: Int) = safeContext.getString(when {
        points < 10 -> R.string.rank_10
        points < 100 -> R.string.rank_100
        points < 200 -> R.string.rank_200
        points < 500 -> R.string.rank_500
        points < 700 -> R.string.rank_700
        points < 1000 -> R.string.rank_1000
        points < 1500 -> R.string.rank_1500
        points < 2000 -> R.string.rank_2000
        points < 3000 -> R.string.rank_3000
        points < 4000 -> R.string.rank_4000
        points < 6000 -> R.string.rank_6000
        points < 8000 -> R.string.rank_8000
        points < 10000 -> R.string.rank_10000
        points < 11000 -> R.string.rank_11000
        points < 12000 -> R.string.rank_12000
        points < 14000 -> R.string.rank_14000
        points < 16000 -> R.string.rank_16000
        points < 18000 -> R.string.rank_18000
        points < 20000 -> R.string.rank_20000
        points > 20000 -> R.string.rank_kami_sama
        else -> throw IllegalArgumentException("Illegal rank: $points")
    })
}
