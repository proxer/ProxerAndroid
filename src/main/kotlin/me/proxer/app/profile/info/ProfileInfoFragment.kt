package me.proxer.app.profile.info

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.forum.TopicActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.profile.ProfileViewModel
import me.proxer.app.profile.ProfileViewModel.UserInfoWrapper
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.linkClicks
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class ProfileInfoFragment : BaseContentFragment<UserInfoWrapper>(R.layout.fragment_profile) {

    companion object {
        private const val RANK_FORUM_ID = "207664"
        private const val RANK_FORUM_CATEGORY_ID = "79"
        private const val RANK_FORUM_TOPIC = "Rangpunkte und Ränge"
        private const val DATE_FORMAT = "%.1f"

        private val rankRegex = Regex(".+", RegexOption.DOT_MATCHES_ALL)

        fun newInstance() = ProfileInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    override val viewModel by sharedViewModel<ProfileViewModel> {
        parametersOf(userId, username)
    }

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

    private val watchedEpisodesContainer: ViewGroup by bindView(R.id.watchedEpisodesContainer)
    private val episodesRow: TextView by bindView(R.id.episodesRow)
    private val minutesRow: TextView by bindView(R.id.minutesRow)
    private val hoursRow: TextView by bindView(R.id.hoursRow)
    private val daysRow: TextView by bindView(R.id.daysRow)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText.linkClicks()
            .map { it.toPrefixedUrlOrNull().toOptional() }
            .filterSome()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { showPage(it) }

        rank.linkClicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                TopicActivity.navigateTo(requireActivity(), RANK_FORUM_ID, RANK_FORUM_CATEGORY_ID, RANK_FORUM_TOPIC)
            }
    }

    override fun showData(data: UserInfoWrapper) {
        super.showData(data)

        val (userInfo, watchedEpisodes) = data

        val totalPoints = userInfo.animePoints + userInfo.mangaPoints + userInfo.uploadPoints +
            userInfo.forumPoints + userInfo.infoPoints + userInfo.miscPoints

        animePointsRow.text = userInfo.animePoints.toString()
        mangaPointsRow.text = userInfo.mangaPoints.toString()
        uploadPointsRow.text = userInfo.uploadPoints.toString()
        forumPointsRow.text = userInfo.forumPoints.toString()
        infoPointsRow.text = userInfo.infoPoints.toString()
        miscellaneousPointsRow.text = userInfo.miscPoints.toString()
        totalPointsRow.text = totalPoints.toString()
        rank.text = rankToString(totalPoints).linkify(web = false, mentions = false, custom = arrayOf(rankRegex))

        if (userInfo.status.isBlank()) {
            statusContainer.isGone = true
        } else {
            val rawText = userInfo.status + " - " + userInfo.lastStatusChange.distanceInWordsToNow(requireContext())

            statusText.text = rawText.linkify(mentions = false)
        }

        if (watchedEpisodes != null) {
            val minutes = watchedEpisodes * 20
            val hours = minutes / 60f
            val days = hours / 24f

            watchedEpisodesContainer.isVisible = true
            episodesRow.text = watchedEpisodes.toString()
            minutesRow.text = minutes.toString()
            hoursRow.text = DATE_FORMAT.format(hours)
            daysRow.text = DATE_FORMAT.format(days)
        } else {
            watchedEpisodesContainer.isVisible = false
        }
    }

    private fun rankToString(points: Int) = requireContext().getString(
        when {
            points < 10 -> R.string.rank_10
            points < 100 -> R.string.rank_100
            points < 200 -> R.string.rank_200
            points < 500 -> R.string.rank_500
            points < 700 -> R.string.rank_700
            points < 1_000 -> R.string.rank_1000
            points < 1_500 -> R.string.rank_1500
            points < 2_000 -> R.string.rank_2000
            points < 3_000 -> R.string.rank_3000
            points < 4_000 -> R.string.rank_4000
            points < 6_000 -> R.string.rank_6000
            points < 8_000 -> R.string.rank_8000
            points < 10_000 -> R.string.rank_10000
            points < 11_000 -> R.string.rank_11000
            points < 12_000 -> R.string.rank_12000
            points < 14_000 -> R.string.rank_14000
            points < 16_000 -> R.string.rank_16000
            points < 18_000 -> R.string.rank_18000
            points < 20_000 -> R.string.rank_20000
            points > 20_000 -> R.string.rank_kami_sama
            else -> error("Illegal rank: $points")
        }
    )
}
