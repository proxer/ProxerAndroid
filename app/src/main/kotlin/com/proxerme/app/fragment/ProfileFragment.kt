package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.IntRange
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkConsumableTextView
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.TimeUtil
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.user.entitiy.UserInfo
import com.proxerme.library.connection.user.request.UserInfoRequest
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProfileFragment : EasyLoadingFragment<UserInfo>() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"
        private const val STATE_USER_INFO = "fragment_profile_state_user_info"

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

    override val section: SectionManager.Section = SectionManager.Section.PROFILE

    private var userId: String? = null
    private var userName: String? = null
    private var userInfo: UserInfo? = null

    private val animePointsRow: TextView by bindView(R.id.animePointsRow)
    private val mangaPointsRow: TextView by bindView(R.id.mangaPointsRow)
    private val uploadPointsRow: TextView by bindView(R.id.uploadPointsRow)
    private val forumPointsRow: TextView by bindView(R.id.forumPointsRow)
    private val infoPointsRow: TextView by bindView(R.id.infoPointsRow)
    private val miscellaneousPointsRow: TextView by bindView(R.id.miscellaneousPointsRow)
    private val totalPointsRow: TextView by bindView(R.id.totalPointsRow)
    private val rank: TextView by bindView(R.id.rank)

    private val statusContainer: ViewGroup by bindView(R.id.statusContainer)
    private val statusText: LinkConsumableTextView by bindView(R.id.statusText)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        userInfo = savedInstanceState?.getParcelable(STATE_USER_INFO)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(STATE_USER_INFO, userInfo)
    }

    override fun constructLoadingRequest(): LoadingRequest<UserInfo> {
        return LoadingRequest(UserInfoRequest(userId, userName))
    }

    override fun save(result: UserInfo) {
        userInfo = result

        (activity as UserActivity).setUserInfo(result)
    }

    override fun clear() {
        userInfo = null
    }

    override fun show() {
        userInfo?.run {
            val totalPoints = animePoints + mangaPoints + uploadPoints + forumPoints +
                    infoPoints + miscPoints

            animePointsRow.text = animePoints.toString()
            mangaPointsRow.text = mangaPoints.toString()
            uploadPointsRow.text = uploadPoints.toString()
            forumPointsRow.text = forumPoints.toString()
            infoPointsRow.text = infoPoints.toString()
            miscellaneousPointsRow.text = miscPoints.toString()
            totalPointsRow.text = totalPoints.toString()
            rank.text = calculateRank(totalPoints)

            if (status.isBlank()) {
                statusContainer.visibility = View.GONE
            } else {
                statusText.text = Utils.buildClickableText(statusText.context, status + " - " +
                        TimeUtil.convertToRelativeReadableTime(context, lastStatusChange),
                        Link.OnClickListener {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                            } catch (exception: ActivityNotFoundException) {
                                context.toast(R.string.link_error_not_found)
                            }
                        })
            }
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
