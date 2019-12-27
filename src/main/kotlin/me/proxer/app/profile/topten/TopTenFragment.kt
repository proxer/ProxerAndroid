package me.proxer.app.profile.topten

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.profile.topten.TopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.multilineSnackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class TopTenFragment : BaseContentFragment<ZippedTopTenResult>(R.layout.fragment_top_ten) {

    companion object {
        fun newInstance() = TopTenFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<TopTenViewModel> { parametersOf(userId, username) }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    private val profileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = profileActivity.userId

    private val username: String?
        get() = profileActivity.username

    private var animeAdapter by Delegates.notNull<TopTenAdapter>()
    private var mangaAdapter by Delegates.notNull<TopTenAdapter>()

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeRecyclerView: RecyclerView by bindView(R.id.animeRecyclerView)
    private val mangaRecyclerView: RecyclerView by bindView(R.id.mangaRecyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animeAdapter = TopTenAdapter()
        mangaAdapter = TopTenAdapter()

        Observable.merge(animeAdapter.clickSubject, mangaAdapter.clickSubject)
            .autoDisposable(this.scope())
            .subscribe { (view, item) ->
                if (item is LocalTopTenEntry.Ucp) {
                    MediaActivity.navigateTo(requireActivity(), item.entryId, item.name, item.category, view)
                } else {
                    MediaActivity.navigateTo(requireActivity(), item.id, item.name, item.category, view)
                }
            }

        Observable.merge(animeAdapter.deleteSubject, mangaAdapter.deleteSubject)
            .autoDisposable(this.scope())
            .subscribe { viewModel.addItemToDelete(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = DeviceUtils.calculateSpanAmount(requireActivity()) + 1

        animeAdapter.glide = GlideApp.with(this)
        mangaAdapter.glide = GlideApp.with(this)

        animeRecyclerView.isNestedScrollingEnabled = false
        animeRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        animeRecyclerView.adapter = animeAdapter

        mangaRecyclerView.isNestedScrollingEnabled = false
        mangaRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        mangaRecyclerView.adapter = mangaAdapter

        viewModel.itemDeletionError.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.multilineSnackbar(
                    getString(R.string.error_topten_entry_removal, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                )
            }
        })
    }

    override fun onDestroyView() {
        animeRecyclerView.layoutManager = null
        animeRecyclerView.adapter = null

        mangaRecyclerView.layoutManager = null
        mangaRecyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: ZippedTopTenResult) {
        super.showData(data)

        animeAdapter.swapDataAndNotifyWithDiffing(data.animeEntries)
        mangaAdapter.swapDataAndNotifyWithDiffing(data.mangaEntries)

        when (animeAdapter.isEmpty()) {
            true -> animeContainer.isGone = true
            false -> animeContainer.isVisible = true
        }

        when (mangaAdapter.isEmpty()) {
            true -> mangaContainer.isGone = true
            false -> mangaContainer.isVisible = true
        }

        if (animeAdapter.isEmpty() && mangaAdapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_top_ten, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        animeAdapter.swapDataAndNotifyWithDiffing(emptyList())
        mangaAdapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
