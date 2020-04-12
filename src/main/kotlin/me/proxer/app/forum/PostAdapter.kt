package me.proxer.app.forum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.forum.PostAdapter.ViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.util.ProxerUrls
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Ruben Gees
 */
class PostAdapter : BaseAdapter<ParsedPost, ViewHolder>() {

    var glide: GlideRequests? = null
    val profileClickSubject: PublishSubject<Pair<ImageView, ParsedPost>> = PublishSubject.create()

    private val heightMap = ConcurrentHashMap<String, Int>()
    private var layoutManager: LayoutManager? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)

        holder.post.destroyWithRetainingViews()
        holder.signature.destroyWithRetainingViews()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val userContainer by bindView<ViewGroup>(R.id.userContainer)
        internal val image by bindView<ImageView>(R.id.image)
        internal val user by bindView<TextView>(R.id.user)
        internal val post by bindView<BBCodeView>(R.id.post)
        internal val signatureDivider by bindView<View>(R.id.signatureDivider)
        internal val signature by bindView<BBCodeView>(R.id.signature)
        internal val date by bindView<TextView>(R.id.date)
        internal val thankYouIcon by bindView<ImageView>(R.id.thankYouIcon)
        internal val thankYou by bindView<TextView>(R.id.thankYou)

        init {
            post.glide = glide
            post.enableEmotions = true
            post.heightMap = heightMap

            signature.glide = glide
            signature.enableEmotions = true
            signature.heightMap = heightMap

            thankYouIcon.setIconicsImage(CommunityMaterial.Icon2.cmd_thumb_up, 32)
        }

        fun bind(item: ParsedPost) {
            userContainer.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(profileClickSubject)

            Observable.merge(post.heightChanges.map { post }, signature.heightChanges.map { signature })
                .autoDisposable(this)
                .subscribe {
                    it.requestLayout()
                    layoutManager?.requestSimpleAnimationsInNextLayout()
                }

            ViewCompat.setTransitionName(image, "post_${item.id}")

            user.text = item.username
            date.text = item.date.distanceInWordsToNow(date.context)
            thankYou.text = item.thankYouAmount.toString()

            post.userId = item.userId
            post.tree = item.parsedMessage

            item.signature.let {
                signature.userId = item.userId

                if (it == null) {
                    signatureDivider.isGone = true
                    signature.isGone = true
                    signature.tree = null
                } else {
                    signatureDivider.isVisible = true
                    signature.isVisible = true
                    signature.tree = it
                }
            }

            bindImage(item)
        }

        private fun bindImage(item: ParsedPost) {
            if (item.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 32, 4, R.attr.colorSecondary)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.logErrors()
                    ?.into(image)
            }
        }
    }
}
