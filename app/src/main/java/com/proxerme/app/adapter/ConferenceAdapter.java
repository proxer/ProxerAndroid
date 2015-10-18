package com.proxerme.app.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.proxerme.app.R;
import com.proxerme.app.util.TimeUtils;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.util.ProxerInfo;

import java.util.Collection;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class ConferenceAdapter extends PagingAdapter<Conference, ConferenceAdapter.ViewHolder> {

    private OnConferenceInteractionListener onConferenceInteractionListener;

    public ConferenceAdapter() {
    }

    public ConferenceAdapter(@NonNull Collection<Conference> list) {
        super(list);
    }

    public ConferenceAdapter(@NonNull Bundle savedInstanceState) {
        super(savedInstanceState);
    }

    @Override
    protected int getItemsOnPage() {
        return ProxerInfo.CONFERENCES_ON_PAGE;
    }

    @Override
    public ConferenceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conference, parent, false));
    }

    @Override
    public void onBindViewHolder(ConferenceAdapter.ViewHolder holder, int position) {
        Conference item = getItemAt(position);
        int participantAmount = item.getParticipantAmount();
        String participantText = participantAmount + " " +
                (participantAmount == 1 ?
                        holder.participants.getContext().getString(R.string.participant_single) :
                        holder.participants.getContext().getString(R.string.participant_multiple));

        holder.topic.setText(item.getTopic());
        holder.time.setText(TimeUtils.convertToRelativeReadableTime(holder.time.getContext(),
                item.getTime()));
        holder.participants.setText(participantText);

        Glide.with(holder.image.getContext()).load(UrlHolder.getUserImage(item.getImageId()))
                .into(holder.image);
    }

    public void setOnConferenceInteractionListener(OnConferenceInteractionListener
                                                           onConferenceInteractionListener) {
        this.onConferenceInteractionListener = onConferenceInteractionListener;
    }

    public static abstract class OnConferenceInteractionListener {
        public void onConferenceClick(@NonNull View v, @NonNull Conference conference) {

        }

        public void onConferenceImageClick(@NonNull View v, @NonNull Conference conference) {

        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.item_conference_image)
        ImageView image;
        @Bind(R.id.item_conference_title)
        TextView topic;
        @Bind(R.id.item_conference_time)
        TextView time;
        @Bind(R.id.item_conference_participants)
        TextView participants;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.item_conference_image)
        void onImageClick(View v) {
            if (onConferenceInteractionListener != null) {
                onConferenceInteractionListener.onConferenceImageClick(v,
                        getItemAt(getLayoutPosition()));
            }
        }

        @OnClick(R.id.item_conference_content)
        void onContentClick(View v) {
            if (onConferenceInteractionListener != null) {
                onConferenceInteractionListener.onConferenceClick(v, getItemAt(getLayoutPosition()));
            }
        }
    }
}
