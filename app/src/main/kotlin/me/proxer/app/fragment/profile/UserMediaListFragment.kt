package me.proxer.app.fragment.profile

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.profile.UserMediaAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.UserMediaListEntry
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class UserMediaListFragment : PagedLoadingFragment<ProxerCall<List<UserMediaListEntry>>, UserMediaListEntry>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"

        fun newInstance(category: Category): UserMediaListFragment {
            return UserMediaListFragment().apply {
                arguments = bundleOf(CATEGORY_ARGUMENT to category)
            }
        }
    }

    override val itemsOnPage = 30
    override val emptyResultMessage = R.string.error_no_data_user_media_list

    private val profileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = profileActivity.userId

    private val username: String?
        get() = profileActivity.username

    private val category: Category
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as Category


    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, StaggeredGridLayoutManager.VERTICAL)
    }

    override val innerAdapter by lazy { UserMediaAdapter(GlideApp.with(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : UserMediaAdapter.UserMediaAdapterCallback {
            override fun onMediaClick(view: View, item: UserMediaListEntry) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.medium.toCategory(),
                        if (imageView.drawable != null) imageView else null)
            }
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<UserMediaListEntry>>().build()
    override fun constructPagedInput(page: Int) = api.user().mediaList(userId, username)
            .page(page)
            .limit(itemsOnPage)
            .category(category)
            .build()
}
