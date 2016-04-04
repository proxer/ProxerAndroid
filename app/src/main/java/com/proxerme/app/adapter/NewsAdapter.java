package com.proxerme.app.adapter;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.proxerme.app.R;
import com.proxerme.app.util.TimeUtils;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;
import com.proxerme.library.util.ProxerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * An adapter for {@link News}, for usage in a {@link RecyclerView}.
 *
 * @author Ruben Gees
 */
public class NewsAdapter extends PagingAdapter<News, NewsAdapter.ViewHolder> {

    private static final String STATE_NEWS_EXTENSION_IDS = "news_extension_ids";
    private static final int ICON_SIZE = 32;
    private static final int ICON_PADDING = 8;
    private static final float ROTATION_HALF = 180f;
    private static final int DESCRIPTION_MAX_LINES = 3;

    private HashMap<String, Boolean> extensionMap;

    private OnNewsInteractionListener onNewsInteractionListener;

    public NewsAdapter() {
        super();
        extensionMap = new HashMap<>(ProxerInfo.NEWS_ON_PAGE * 2);
    }

    public NewsAdapter(@NonNull Collection<News> news) {
        super(news);
        extensionMap = new HashMap<>(news.size() * 2);
    }

    public NewsAdapter(@NonNull Bundle savedInstanceState) {
        super(savedInstanceState);
        List<String> ids = savedInstanceState.getStringArrayList(STATE_NEWS_EXTENSION_IDS);
        extensionMap = new HashMap<>();

        if (ids != null) {
            for (String id : ids) {
                extensionMap.put(id, true);
            }
        }
    }

    @Override
    public NewsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false));
    }

    @Override
    public void onBindViewHolder(NewsAdapter.ViewHolder holder, int position) {
        News item = getItemAt(position);

        holder.title.setText(item.getSubject().trim());
        holder.description.setText(item.getDescription().trim());
        holder.category.setText(item.getCategoryTitle());
        holder.time.setText(TimeUtils.convertToRelativeReadableTime(holder.time.getContext(),
                item.getTime()));

        holder.expand.setImageDrawable(new IconicsDrawable(holder.expand.getContext())
                .colorRes(R.color.icons_grey).sizeDp(ICON_SIZE).paddingDp(ICON_PADDING)
                .icon(CommunityMaterial.Icon.cmd_chevron_down));

        if (extensionMap.containsKey(item.getId())) {
            holder.description.setMaxLines(Integer.MAX_VALUE);
            ViewCompat.setRotationX(holder.expand, ROTATION_HALF);
        } else {
            holder.description.setMaxLines(DESCRIPTION_MAX_LINES);
            ViewCompat.setRotationX(holder.expand, 0f);
        }

        Glide.with(holder.image.getContext()).load(UrlHolder.getNewsImageUrl(item.getId(),
                item.getImageId())).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(holder.image);
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        super.saveInstanceState(outState);
        outState.putStringArrayList(STATE_NEWS_EXTENSION_IDS,
                new ArrayList<>(extensionMap.keySet()));
    }

    @Override
    @IntRange(from = 1)
    protected int getItemsOnPage() {
        return ProxerInfo.NEWS_ON_PAGE;
    }

    public void setOnNewsInteractionListener(@Nullable OnNewsInteractionListener
                                                     onNewsInteractionListener) {
        this.onNewsInteractionListener = onNewsInteractionListener;
    }

    public static abstract class OnNewsInteractionListener {
        public void onNewsClick(@NonNull View v, @NonNull News news) {

        }

        public void onNewsImageClick(@NonNull View v, @NonNull News news) {

        }

        public void onNewsExpanded(@NonNull View v, @NonNull News news) {

        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.item_news_image)
        ImageView image;
        @Bind(R.id.item_news_title)
        TextView title;
        @Bind(R.id.item_news_description)
        TextView description;
        @Bind(R.id.item_news_category)
        TextView category;
        @Bind(R.id.item_news_time)
        TextView time;
        @Bind(R.id.item_news_expand_description)
        ImageButton expand;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.item_news_content_container)
        void onContentClick(View view) {
            if (onNewsInteractionListener != null) {
                onNewsInteractionListener.onNewsClick(view, getItemAt(getAdapterPosition()));
            }
        }

        @OnClick(R.id.item_news_image)
        void onImageClick(View view) {
            if (onNewsInteractionListener != null) {
                onNewsInteractionListener.onNewsImageClick(view, getItemAt(getAdapterPosition()));
            }
        }

        @OnClick(R.id.item_news_expand_description)
        void onExpandClick(View view) {
            News news = getItemAt(getAdapterPosition());
            String id = news.getId();
            boolean isExpanded = extensionMap.containsKey(id);

            if (isExpanded) {
                extensionMap.remove(id);

                description.setMaxLines(DESCRIPTION_MAX_LINES);
                ViewCompat.animate(view).rotationX(0f);
            } else {
                extensionMap.put(id, true);

                description.setMaxLines(Integer.MAX_VALUE);
                ViewCompat.animate(view).rotationX(ROTATION_HALF);

                if (onNewsInteractionListener != null) {
                    onNewsInteractionListener.onNewsExpanded(view, news);
                }
            }
        }
    }
}
