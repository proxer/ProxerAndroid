package me.proxer.app.ucp.topten

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.ucp.topten.UcpTopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class UcpTopTenFragment : BaseContentFragment<ZippedTopTenResult>() {

    companion object {
        fun newInstance() = UcpTopTenFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { UcpTopTenViewModelProvider.get(this) }

    private var animeAdapter by Delegates.notNull<UcpTopTenAdapter>()
    private var mangaAdapter by Delegates.notNull<UcpTopTenAdapter>()

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeRecyclerView: RecyclerView by bindView(R.id.animeRecyclerView)
    private val mangaRecyclerView: RecyclerView by bindView(R.id.mangaRecyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animeAdapter = UcpTopTenAdapter()
        mangaAdapter = UcpTopTenAdapter()

        Observable.merge(animeAdapter.clickSubject, mangaAdapter.clickSubject)
            .autoDispose(this)
            .subscribe { (view, item) ->
                MediaActivity.navigateTo(requireActivity(), item.entryId, item.name, item.category,
                    if (view.drawable != null) view else null)
            }

        Observable.merge(animeAdapter.deleteSubject, mangaAdapter.deleteSubject)
            .autoDispose(this)
            .subscribe { viewModel.addItemToDelete(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_top_ten, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = DeviceUtils.calculateSpanAmount(requireActivity()) + 1

        animeAdapter.glide = GlideApp.with(this)
        mangaAdapter.glide = GlideApp.with(this)

        viewModel.itemDeletionError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_topten_entry_removal, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity))
            }
        })

        animeRecyclerView.isNestedScrollingEnabled = false
        animeRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        animeRecyclerView.adapter = animeAdapter

        mangaRecyclerView.isNestedScrollingEnabled = false
        mangaRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        mangaRecyclerView.adapter = mangaAdapter
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
            true -> animeContainer.visibility = View.GONE
            false -> animeContainer.visibility = View.VISIBLE
        }

        when (mangaAdapter.isEmpty()) {
            true -> mangaContainer.visibility = View.GONE
            false -> mangaContainer.visibility = View.VISIBLE
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
