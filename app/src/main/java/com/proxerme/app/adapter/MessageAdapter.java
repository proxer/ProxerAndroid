package com.proxerme.app.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.proxerme.app.R;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.entity.Message;
import com.proxerme.library.util.ProxerInfo;

import java.util.Collection;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MessageAdapter extends PagingAdapter<Message, MessageAdapter.MessageViewHolder> {

    private static final int TYPE_MESSAGE_SELF = 0;
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_MESSAGE_WITH_TITLE = 2;
    private static final int TYPE_MESSAGE_PROFILE_IMAGE = 3;
    private static final int TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE = 4;
    private static final int TYPE_ACTION = 5;

    @Nullable
    private LoginUser user;

    private OnMessageInteractionListener onMessageInteractionListener;

    public MessageAdapter(@Nullable LoginUser user) {
        this.user = user;
    }

    public MessageAdapter(@NonNull Collection<Message> list, @Nullable LoginUser user) {
        super(list);

        this.user = user;
    }

    public MessageAdapter(@NonNull Bundle savedInstanceState, @Nullable LoginUser user) {
        super(savedInstanceState);

        this.user = user;
    }

    @Override
    protected int getItemsOnPage() {
        return ProxerInfo.MESSAGES_ON_PAGE;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_MESSAGE_SELF:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_self, parent,
                        false));
            case TYPE_MESSAGE:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, parent,
                        false));
            case TYPE_MESSAGE_WITH_TITLE:
                return new MessageTitleViewHolder(inflater.inflate(R.layout.item_message_with_title,
                        parent, false));
            case TYPE_MESSAGE_PROFILE_IMAGE:
                return new MessageImageViewHolder(inflater.inflate(
                        R.layout.item_message_profile_image, parent, false));
            case TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE:
                return new MessageImageTitleViewHolder(inflater.inflate(
                        R.layout.item_message_profile_image_with_title, parent, false));
            case TYPE_ACTION:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_action, parent,
                        false));
            default:
                throw new RuntimeException("An unknown viewType was passed: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        if (holder.getClass().equals(MessageTitleViewHolder.class)) {
            ((MessageTitleViewHolder) holder).title.setText(getItemAt(position).getUsername());
        } else if (holder.getClass().equals(MessageImageTitleViewHolder.class)) {
            MessageImageTitleViewHolder castedHolder = (MessageImageTitleViewHolder) holder;

            castedHolder.title.setText(getItemAt(position).getUsername());
            Glide.with(castedHolder.image.getContext())
                    .load(UrlHolder.getUserImageUrl(getItemAt(position).getImageId()))
                    .into(castedHolder.image);
        } else if (holder.getClass().equals(MessageImageViewHolder.class)) {
            MessageImageViewHolder castedHolder = (MessageImageViewHolder) holder;

            Glide.with(castedHolder.image.getContext())
                    .load(UrlHolder.getUserImageUrl(getItemAt(position).getImageId()))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(castedHolder.image);
        }

        holder.message.setText(getItemAt(position).getMessage());
    }

    @Override
    public int getItemViewType(int position) {
        if (user == null) {
            throw new RuntimeException("This method should not be called if the user is null");
        }

        Message current = getItemAt(position);

        if (current.getAction() != null) {
            return TYPE_ACTION;
        } else if (current.getFromId().equals(user.getId())) {
            return TYPE_MESSAGE_SELF;
        } else {
            if (position - 1 < 0) {
                if (position + 1 >= getItemCount()) {
                    return TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE;
                } else {
                    if (getItemAt(position + 1).getFromId().equals(current.getId())
                            && getItemAt(position + 1).getAction() == null) {
                        return TYPE_MESSAGE_WITH_TITLE;
                    } else {
                        return TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE;
                    }
                }
            } else if (position + 1 >= getItemCount()) {
                if (getItemAt(position - 1).getFromId().equals(current.getId())
                        && getItemAt(position - 1).getAction() == null) {
                    return TYPE_MESSAGE_PROFILE_IMAGE;
                } else {
                    return TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE;
                }
            } else {
                if (getItemAt(position - 1).getFromId().equals(current.getId())
                        && getItemAt(position - 1).getAction() == null) {
                    if (getItemAt(position + 1).getFromId().equals(current.getId())
                            && getItemAt(position + 1).getAction() == null) {
                        return TYPE_MESSAGE;
                    } else {
                        return TYPE_MESSAGE_PROFILE_IMAGE;
                    }
                } else {
                    if (getItemAt(position + 1).getFromId().equals(current.getId())
                            && getItemAt(position + 1).getAction() == null) {
                        return TYPE_MESSAGE_WITH_TITLE;
                    } else {
                        return TYPE_MESSAGE_PROFILE_IMAGE_WITH_TITLE;
                    }
                }
            }
        }
    }

    public void setUser(@Nullable LoginUser user) {
        this.user = user;
    }

    public void setOnMessageInteractionListener(@Nullable OnMessageInteractionListener
                                                        onMessageInteractionListener) {
        this.onMessageInteractionListener = onMessageInteractionListener;
    }

    public static abstract class OnMessageInteractionListener {
        public void onMessageImageClick(@NonNull View v, @NonNull Message message) {

        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.item_message_message)
        TextView message;

        public MessageViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

    public class MessageTitleViewHolder extends MessageViewHolder {

        @Bind(R.id.item_message_title)
        TextView title;

        public MessageTitleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class MessageImageTitleViewHolder extends MessageViewHolder {

        @Bind(R.id.item_message_title)
        TextView title;
        @Bind(R.id.item_message_image)
        CircleImageView image;

        public MessageImageTitleViewHolder(View itemView) {
            super(itemView);
        }

        @OnClick(R.id.item_message_image)
        public void onImageClick(View v) {
            if (onMessageInteractionListener != null) {
                onMessageInteractionListener.onMessageImageClick(v,
                        getItemAt(getAdapterPosition()));
            }
        }
    }

    public class MessageImageViewHolder extends MessageViewHolder {

        @Bind(R.id.item_message_image)
        CircleImageView image;

        public MessageImageViewHolder(View itemView) {
            super(itemView);
        }

        @OnClick(R.id.item_message_image)
        public void onImageClick(View v) {
            if (onMessageInteractionListener != null) {
                onMessageInteractionListener.onMessageImageClick(v,
                        getItemAt(getAdapterPosition()));
            }
        }
    }
}
