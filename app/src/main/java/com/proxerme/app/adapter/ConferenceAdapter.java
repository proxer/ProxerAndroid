package com.proxerme.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.proxerme.library.entity.Conference;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class ConferenceAdapter extends PagingAdapter<Conference, ConferenceAdapter.ViewHolder> {
    @Override
    protected int getItemsOnPage() {
        return 0;
    }

    @Override
    public ConferenceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ConferenceAdapter.ViewHolder holder, int position) {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView topic;

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
