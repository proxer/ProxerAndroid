package com.rubengees.proxerme.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.connection.UrlHolder;
import com.rubengees.proxerme.entity.News;
import com.rubengees.proxerme.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static com.rubengees.proxerme.manager.NewsManager.NEWS_ON_PAGE;
import static com.rubengees.proxerme.manager.NewsManager.OFFSET_NOT_CALCULABLE;
import static com.rubengees.proxerme.manager.NewsManager.calculateOffsetFromEnd;
import static com.rubengees.proxerme.manager.NewsManager.calculateOffsetFromStart;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private static final String STATE_NEWS_LIST = "news_list";
    private static final String STATE_NEWS_EXTENSION_IDS = "news_extension_ids";
    private static final int ICON_SIZE = 32;
    private static final int ICON_PADDING = 8;
    private static final float ROTATION_HALF = 180f;
    private static final int DESCRIPTION_MAX_LINES = 3;

    private ArrayList<News> list;
    private HashMap<Integer, Boolean> extensionMap;

    private OnNewsInteractionListener onNewsInteractionListener;

    public NewsAdapter() {
        this.list = new ArrayList<>(NEWS_ON_PAGE * 2);
        extensionMap = new HashMap<>(NEWS_ON_PAGE * 2);
    }

    public NewsAdapter(Collection<News> news) {
        this.list = new ArrayList<>(news.size() * 2);
        extensionMap = new HashMap<>(news.size() * 2);

        this.list.addAll(news);
        notifyItemRangeInserted(0, news.size());
    }

    public NewsAdapter(@NonNull Bundle savedInstanceState) {
        this.list = savedInstanceState.getParcelableArrayList(STATE_NEWS_LIST);
        List<Integer> ids = savedInstanceState.getIntegerArrayList(STATE_NEWS_EXTENSION_IDS);
        extensionMap = new HashMap<>(this.list.size() * 2);

        if (ids != null) {
            for (Integer id : ids) {
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
        News item = list.get(position);

        holder.title.setText(item.getSubject());
        holder.description.setText(item.getDescription());
        holder.category.setText(item.getCategoryTitle());
        holder.time.setText(TimeUtils.convertToRelativeReadableTime(holder.time.getContext(),
                item.getTime()));

        holder.expand.setImageDrawable(new IconicsDrawable(holder.expand.getContext())
                .colorRes(R.color.icons_grey).sizeDp(ICON_SIZE).paddingDp(ICON_PADDING)
                .icon(GoogleMaterial.Icon.gmd_keyboard_arrow_down));

        if (extensionMap.containsKey(item.getId())) {
            holder.description.setMaxLines(Integer.MAX_VALUE);
            ViewCompat.setRotationX(holder.expand, ROTATION_HALF);
        } else {
            holder.description.setMaxLines(DESCRIPTION_MAX_LINES);
            ViewCompat.setRotationX(holder.expand, 0f);
        }

        Glide.with(holder.image.getContext()).load(UrlHolder.getNewsImageUrl(item.getId(),
                item.getImageId())).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Inserts the given List of news into this Adapter. A offset to the existing data is calculated,
     * to determine if more data needs to be loaded.
     *
     * @param news The List of News to insert.
     * @return The offset to the existing data. -1 is returned, when the offset is to large and
     * new data needs to be loaded. -2 is returned if the offset could not ne calculated (The
     * internal List or the passed List is empty)
     */
    public int insertAtStart(@NonNull List<News> news) {
        if (!news.isEmpty()) {
            int offset = calculateOffsetFromStart(news, this.list.get(0).getId());

            if (offset >= 0) {
                news = news.subList(0, offset);
            }

            this.list.addAll(0, news);
            notifyItemRangeInserted(0, news.size());

            return offset;
        }

        return OFFSET_NOT_CALCULABLE;
    }

    public int append(@NonNull List<News> news) {
        if (!news.isEmpty()) {
            int offset = calculateOffsetFromEnd(this.list, news.get(0));

            if (offset > 0) {
                news = news.subList(offset, news.size());
            }

            this.list.addAll(news);
            notifyItemRangeInserted(this.list.size() - news.size(), news.size());

            return offset;
        }

        return OFFSET_NOT_CALCULABLE;
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(STATE_NEWS_LIST, list);
        outState.putIntegerArrayList(STATE_NEWS_EXTENSION_IDS, new ArrayList<>(extensionMap.keySet()));
    }

    public void setOnNewsInteractionListener(OnNewsInteractionListener onNewsInteractionListener) {
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

        ImageView image;
        TextView title;
        TextView description;
        TextView category;
        TextView time;

        ImageButton expand;

        public ViewHolder(View itemView) {
            super(itemView);

            image = (ImageView) itemView.findViewById(R.id.item_news_image);
            title = (TextView) itemView.findViewById(R.id.item_news_title);
            description = (TextView) itemView.findViewById(R.id.item_news_description);
            category = (TextView) itemView.findViewById(R.id.item_news_category);
            time = (TextView) itemView.findViewById(R.id.item_news_time);
            expand = (ImageButton) itemView.findViewById(R.id.item_news_expand_description);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onNewsInteractionListener != null) {
                        onNewsInteractionListener.onNewsClick(v, list.get(getLayoutPosition()));
                    }
                }
            });

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onNewsInteractionListener != null) {
                        onNewsInteractionListener.onNewsImageClick(v, list.get(getLayoutPosition()));
                    }
                }
            });

            expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    News news = list.get(getLayoutPosition());
                    int id = news.getId();
                    boolean isExpanded = extensionMap.containsKey(id);

                    if (isExpanded) {
                        extensionMap.remove(id);

                        description.setMaxLines(DESCRIPTION_MAX_LINES);
                        ViewCompat.animate(v).rotationX(0f);
                    } else {
                        extensionMap.put(id, true);

                        description.setMaxLines(Integer.MAX_VALUE);
                        ViewCompat.animate(v).rotationX(ROTATION_HALF);

                        if (onNewsInteractionListener != null) {
                            onNewsInteractionListener.onNewsExpanded(v, news);
                        }
                    }
                }
            });
        }
    }
}
