package com.proxerme.app.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.proxerme.app.util.PagingHelper;
import com.proxerme.library.interfaces.IdItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An abstract Adapter for page based item Lists.
 *
 * @author Ruben Gees
 */
public abstract class PagingAdapter<T extends IdItem & Parcelable,
        V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    private static final String STATE_LIST = "paging_list";

    private ArrayList<T> list;

    public PagingAdapter() {
        this.list = new ArrayList<>(getItemsOnPage() * 2);
    }

    public PagingAdapter(@NonNull Collection<T> list) {
        this.list = new ArrayList<>(list.size() * 2);

        this.list.addAll(list);
        notifyItemRangeInserted(0, list.size());
    }

    public PagingAdapter(@NonNull Bundle savedInstanceState) {
        this.list = savedInstanceState.getParcelableArrayList(STATE_LIST);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public T getItemAt(int position) {
        return list.get(position);
    }

    public void setItemAt(int position, T item) {
        list.set(position, item);
    }

    /**
     * Inserts a List of items into the Adapter, removing the existing ones. The items are inserted
     * at the first position.
     *
     * @param list The List of items.
     * @return The offset to the existing items.
     */
    public int insertAtStart(@NonNull List<T> list) {
        if (this.list.isEmpty()) {
            append(list);
        }

        if (!list.isEmpty()) {
            int offset = PagingHelper.calculateOffsetFromStart(list, this.list.get(0),
                    getItemsOnPage());

            if (offset >= 0) {
                list = list.subList(0, offset);
            }

            this.list.addAll(0, list);
            notifyItemRangeInserted(0, list.size());

            return offset;
        }

        return PagingHelper.OFFSET_NOT_CALCULABLE;
    }

    /**
     * Appends a List of items to the Adapter, removing the existing ones. The items are appended at
     * the last position.
     *
     * @param list The List of items.
     * @return The offset to the existing items.
     */
    public int append(@NonNull List<T> list) {
        if (!list.isEmpty()) {
            int offset = PagingHelper.calculateOffsetFromEnd(this.list, list.get(0),
                    getItemsOnPage());

            if (offset > 0) {
                list = list.subList(offset, list.size());
            }

            this.list.addAll(list);
            notifyItemRangeInserted(this.list.size() - list.size(), list.size());

            return offset;
        }

        return PagingHelper.OFFSET_NOT_CALCULABLE;
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(STATE_LIST, list);
    }

    /**
     * Returns the items on a Page of the inheriting Adapter type.
     *
     * @return The items on a page.
     */
    @IntRange(from = 1)
    protected abstract int getItemsOnPage();

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void clear() {
        int count = list.size();

        list.clear();
        notifyItemRangeRemoved(0, count);
    }
}
