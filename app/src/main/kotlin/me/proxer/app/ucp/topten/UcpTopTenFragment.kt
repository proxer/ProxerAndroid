package me.proxer.app.ucp.topten

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.ucp.topten.UcpTopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class UcpTopTenFragment : BaseContentFragment<ZippedTopTenResult>() {

    companion object {
        fun newInstance() = UcpTopTenFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel: UcpTopTenViewModel by unsafeLazy {
        ViewModelProviders.of(this).get(UcpTopTenViewModel::class.java)
    }

    private lateinit var animeAdapter: UcpTopTenAdapter
    private lateinit var mangaAdapter: UcpTopTenAdapter

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeRecyclerView: RecyclerView by bindView(R.id.animeRecyclerView)
    private val mangaRecyclerView: RecyclerView by bindView(R.id.mangaRecyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animeAdapter = UcpTopTenAdapter()
        mangaAdapter = UcpTopTenAdapter()

        Observable.merge(animeAdapter.clickSubject, mangaAdapter.clickSubject)
                .bindToLifecycle(this)
                .subscribe { (view, item) ->
                    MediaActivity.navigateTo(activity, item.entryId, item.name, item.category,
                            if (view.drawable != null) view else null)
                }

        Observable.merge(animeAdapter.deleteSubject, mangaAdapter.deleteSubject)
                .bindToLifecycle(this)
                .subscribe { viewModel.addItemToDelete(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_top_ten, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = DeviceUtils.calculateSpanAmount(activity) + 1

        animeAdapter.glide = GlideApp.with(this)
        mangaAdapter.glide = GlideApp.with(this)

        viewModel.itemDeletionError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_topten_entry_removal, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
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

        animeAdapter.swapDataAndNotifyChange(data.animeEntries)
        mangaAdapter.swapDataAndNotifyChange(data.mangaEntries)

        when (animeAdapter.isEmpty()) {
            true -> animeContainer.visibility = View.GONE
            false -> animeContainer.visibility = View.VISIBLE
        }

        when (mangaAdapter.isEmpty()) {
            true -> mangaContainer.visibility = View.GONE
            false -> mangaContainer.visibility = View.VISIBLE
        }

        if (animeAdapter.isEmpty() && mangaAdapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_top_ten, ErrorAction.ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        animeAdapter.clearAndNotifyRemoval()
        mangaAdapter.clearAndNotifyRemoval()

        super.hideData()
    }
}
