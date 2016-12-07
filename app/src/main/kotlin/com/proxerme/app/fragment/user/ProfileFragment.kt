package com.proxerme.app.fragment.user

import android.os.Bundle
import android.support.annotation.IntRange
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.util.TimeUtil
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.user.entitiy.UserInfo
import com.proxerme.library.connection.user.request.UserInfoRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProfileFragment : SingleLoadingFragment<UserInfo>() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"

        fun newInstance(userId: String? = null, userName: String? = null): ProfileFragment {
            if (userId.isNullOrBlank() && userName.isNullOrBlank()) {
                throw IllegalArgumentException("You must provide at least one of the arguments")
            }

            return ProfileFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_USER_ID, userId)
                    this.putString(ARGUMENT_USER_NAME, userName)
                }
            }
        }
    }

    override val section = Section.PROFILE

    private val userId: String?
        get() = arguments.getString(ARGUMENT_USER_ID)
    private val userName: String?
        get() = arguments.getString(ARGUMENT_USER_NAME)

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

    override fun constructTask(): ListenableTask<UserInfo> {
        return ProxerLoadingTask({ UserInfoRequest(userId, userName) })
    }

    override fun present(data: UserInfo) {
        (activity as UserActivity).setUserInfo(data)

        val totalPoints = data.animePoints + data.mangaPoints + data.uploadPoints +
                data.forumPoints + data.infoPoints + data.miscPoints

        animePointsRow.text = data.animePoints.toString()
        mangaPointsRow.text = data.mangaPoints.toString()
        uploadPointsRow.text = data.uploadPoints.toString()
        forumPointsRow.text = data.forumPoints.toString()
        infoPointsRow.text = data.infoPoints.toString()
        miscellaneousPointsRow.text = data.miscPoints.toString()
        totalPointsRow.text = totalPoints.toString()
        rank.text = calculateRank(totalPoints)

        if (data.status.isBlank()) {
            statusContainer.visibility = View.GONE
        } else {
            statusText.text = Utils.buildClickableText(statusText.context, data.status + " - " +
                    TimeUtil.convertToRelativeReadableTime(context, data.lastStatusChange),
                    Link.OnClickListener { link ->
                        Utils.viewLink(context, link + "?device=mobile")
                    })
        }
    }

    private fun calculateRank(@IntRange(from = 0) points: Int): String {
        when {
            (points < 10) -> return "Schnupperninja"
            (points < 100) -> return "Anwärter"
            (points < 200) -> return "Akademie Schüler"
            (points < 500) -> return "Genin"
            (points < 700) -> return "Chunin"
            (points < 1000) -> return "Jonin"
            (points < 1500) -> return "Anbu"
            (points < 2000) -> return "Spezial Anbu"
            (points < 3000) -> return "Medizin Ninja"
            (points < 4000) -> return "Sannin"
            (points < 6000) -> return "Ninja Meister"
            (points < 8000) -> return "Kage"
            (points < 10000) -> return "Hokage"
            (points < 11000) -> return "Otaku"
            (points < 12000) -> return "Otaku no Senpai"
            (points < 14000) -> return "Otaku no Sensei"
            (points < 16000) -> return "Otaku no Shihan"
            (points < 18000) -> return "Hikikomori"
            (points < 20000) -> return "Halbgott"
            (points > 20000) -> return "Kami-Sama"
            else -> throw RuntimeException("No negative values allowed")
        }
    }
}
