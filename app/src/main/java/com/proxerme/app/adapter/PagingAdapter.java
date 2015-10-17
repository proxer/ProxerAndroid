package com.proxerme.app.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.proxerme.app.util.PagingHelper;
import com.proxerme.library.interfaces.IdItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by geesr on 17.10.2015.
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

    public int insertAtStart(@NonNull List<T> list) {
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

    protected abstract int getItemsOnPage();
}
