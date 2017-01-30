package com.proxerme.app.fragment.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.fragment.user.ProfileFragment.ProfileInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.ParameterMapper
import com.proxerme.app.util.TimeUtils
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.user.entitiy.UserInfo
import com.proxerme.library.connection.user.request.UserInfoRequest
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProfileFragment : SingleLoadingFragment<ProfileInput, UserInfo>() {

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }

    override val section = Section.PROFILE

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

    override fun present(data: UserInfo) {
        profileActivity.userId = data.id
        profileActivity.username = data.username
        profileActivity.imageId = data.imageId

        val totalPoints = data.animePoints + data.mangaPoints + data.uploadPoints +
                data.forumPoints + data.infoPoints + data.miscPoints

        animePointsRow.text = data.animePoints.toString()
        mangaPointsRow.text = data.mangaPoints.toString()
        uploadPointsRow.text = data.uploadPoints.toString()
        forumPointsRow.text = data.forumPoints.toString()
        infoPointsRow.text = data.infoPoints.toString()
        miscellaneousPointsRow.text = data.miscPoints.toString()
        totalPointsRow.text = totalPoints.toString()
        rank.text = ParameterMapper.rank(context, totalPoints)

        if (data.status.isBlank()) {
            statusContainer.visibility = View.GONE
        } else {
            statusText.text = Utils.buildClickableText(statusText.context, data.status + " - " +
                    TimeUtils.convertToRelativeReadableTime(context, data.lastStatusChange),
                    Link.OnClickListener { link ->
                        showPage(HttpUrl.parse(link).newBuilder()
                                .addQueryParameter("device", "mobile")
                                .build())
                    })
        }
    }

    override fun constructTask(): Task<ProfileInput, UserInfo> {
        return ProxerLoadingTask({ UserInfoRequest(userId, username) })
    }

    override fun constructInput(): ProfileInput {
        return ProfileInput(userId, username)
    }

    class ProfileInput(val userId: String?, val username: String?)
}
