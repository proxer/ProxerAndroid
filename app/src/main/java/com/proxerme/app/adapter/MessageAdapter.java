package com.proxerme.app.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.proxerme.app.R;
import com.proxerme.app.util.TimeUtils;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.entity.Message;
import com.proxerme.library.util.ProxerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MessageAdapter extends PagingAdapter<Message, MessageAdapter.MessageViewHolder> {

    private static final String STATE_MESSAGE_SHOWING_TIME_IDS = "message_showing_time_ids";

    private static final int TYPE_MESSAGE_INNER = 0;
    private static final int TYPE_MESSAGE_SINGLE = 1;
    private static final int TYPE_MESSAGE_TOP = 2;
    private static final int TYPE_MESSAGE_BOTTOM = 3;
    private static final int TYPE_MESSAGE_SELF_INNER = 4;
    private static final int TYPE_MESSAGE_SELF_SINGLE = 5;
    private static final int TYPE_MESSAGE_SELF_TOP = 6;
    private static final int TYPE_MESSAGE_SELF_BOTTOM = 7;
    private static final int TYPE_ACTION = 8;

    @Nullable
    private LoginUser user;

    private HashMap<String, Boolean> showingTimeMap;

    private OnMessageInteractionListener onMessageInteractionListener;

    public MessageAdapter(@Nullable LoginUser user) {
        this.user = user;
        this.showingTimeMap = new HashMap<>(ProxerInfo.MESSAGES_ON_PAGE * 2);
    }

    public MessageAdapter(@NonNull Collection<Message> list, @Nullable LoginUser user) {
        super(list);

        this.user = user;
        this.showingTimeMap = new HashMap<>(list.size() * 2);
    }

    public MessageAdapter(@NonNull Bundle savedInstanceState, @Nullable LoginUser user) {
        super(savedInstanceState);

        this.user = user;

        List<String> ids = savedInstanceState.getStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS);
        this.showingTimeMap = new HashMap<>();

        if (ids != null) {
            for (String id : ids) {
                this.showingTimeMap.put(id, true);
            }
        }
    }

    @Override
    protected int getItemsOnPage() {
        return ProxerInfo.MESSAGES_ON_PAGE;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_MESSAGE_INNER:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_inner, parent,
                        false));
            case TYPE_MESSAGE_SINGLE:
                return new MessageImageTitleViewHolder(inflater
                        .inflate(R.layout.item_message_single, parent, false));
            case TYPE_MESSAGE_TOP:
                return new MessageImageTitleViewHolder(inflater
                        .inflate(R.layout.item_message_top, parent, false));
            case TYPE_MESSAGE_BOTTOM:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_bottom, parent,
                        false));
            case TYPE_MESSAGE_SELF_INNER:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_self_inner,
                        parent, false));
            case TYPE_MESSAGE_SELF_SINGLE:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_self_single,
                        parent, false));
            case TYPE_MESSAGE_SELF_TOP:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_self_top,
                        parent, false));
            case TYPE_MESSAGE_SELF_BOTTOM:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_self_bottom,
                        parent, false));
            case TYPE_ACTION:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_action, parent,
                        false));
            default:
                throw new RuntimeException("An unknown viewType was passed: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message current = getItemAt(position);

        if (holder.getClass().equals(MessageImageTitleViewHolder.class)) {
            MessageImageTitleViewHolder castedHolder = (MessageImageTitleViewHolder) holder;

            castedHolder.title.setText(current.getUsername());

            if (TextUtils.isEmpty(current.getImageId())) {
                castedHolder.image
                        .setImageDrawable(new IconicsDrawable(castedHolder.image.getContext())
                                .icon(CommunityMaterial.Icon.cmd_account).sizeDp(32).paddingDp(32)
                                .colorRes(R.color.colorPrimary));
            } else {
                Glide.with(castedHolder.image.getContext())
                        .load(UrlHolder.getUserImageUrl(current.getImageId()))
                        .into(castedHolder.image);
            }
        }

        holder.message.setText(current.getMessage());
        holder.time.setText(TimeUtils.convertToRelativeReadableTime(holder.time.getContext(),
                current.getTime()));

        Linkify.addLinks(holder.message, Linkify.ALL);

        if (showingTimeMap.containsKey(current.getId())) {
            holder.time.setVisibility(View.VISIBLE);
        } else {
            holder.time.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (user == null) {
            throw new RuntimeException("This method should not be called if the user is null");
        }

        int result;
        Message current = getItemAt(position);

        if (current.getAction() != null) {
            result = TYPE_ACTION;
        } else {
            if (position - 1 < 0) {
                if (position + 1 >= getItemCount()) {
                    result = TYPE_MESSAGE_SINGLE; // The item is the only one
                } else {
                    if (getItemAt(position + 1).getFromId().equals(current.getFromId())
                            && getItemAt(position + 1).getAction() == null) {
                        result = TYPE_MESSAGE_BOTTOM; // The item is the bottommost item and has an item from the same user above
                    } else {
                        result = TYPE_MESSAGE_SINGLE; // The item is the bottommost item and doesn't have an item from the same user above
                    }
                }
            } else if (position + 1 >= getItemCount()) {
                if (getItemAt(position - 1).getFromId().equals(current.getFromId())
                        && getItemAt(position - 1).getAction() == null) {
                    result = TYPE_MESSAGE_TOP; // The item is the topmost item and has an item from the same user beneath
                } else {
                    result = TYPE_MESSAGE_SINGLE; // The item is the topmost item and doesn't have an item from the same user beneath
                }
            } else {
                if (getItemAt(position - 1).getFromId().equals(current.getFromId())
                        && getItemAt(position - 1).getAction() == null) {
                    if (getItemAt(position + 1).getFromId().equals(current.getFromId())
                            && getItemAt(position + 1).getAction() == null) {
                        result = TYPE_MESSAGE_INNER; // The item is in between two other items from the same user
                    } else {
                        result = TYPE_MESSAGE_TOP; // The item has an item from the same user beneath but not above
                    }
                } else {
                    if (getItemAt(position + 1).getFromId().equals(current.getFromId())
                            && getItemAt(position + 1).getAction() == null) {
                        result = TYPE_MESSAGE_BOTTOM; // The item has an item from the same user above but not beneath
                    } else {
                        result = TYPE_MESSAGE_SINGLE;  // The item stands alone
                    }
                }
            }

            if (current.getFromId().equals(user.getId())) {
                result += 4; // Make the item a "self" item
            }
        }

        return result;
    }

    @Override
    public int insertAtStart(@NonNull List<Message> list) {
        int offset = super.insertAtStart(list);

        if (offset != PagingHelper.OFFSET_NOT_CALCULABLE) {
            if (offset == PagingHelper.OFFSET_TOO_LARGE) {
                notifyItemChanged(list.size());
            } else if (offset > 0) {
                notifyItemChanged(offset);
            }
        }

        return offset;
    }

    @Override
    public int append(@NonNull List<Message> list) {
        int offset = super.append(list);

        if (offset != PagingHelper.OFFSET_NOT_CALCULABLE) {
            if (offset == PagingHelper.OFFSET_TOO_LARGE) {
                notifyItemChanged(getItemCount() - list.size());
            } else if (offset > 0) {
                notifyItemChanged(getItemCount() - offset);
            }
        }

        return offset;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        super.saveInstanceState(outState);

        outState.putStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS,
                new ArrayList<>(showingTimeMap.keySet()));
    }

    @Override
    public void clear() {
        super.clear();

        user = null;
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

        @BindView(R.id.item_message_message)
        TextView message;
        @BindView(R.id.item_message_time)
        TextView time;

        public MessageViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.item_message_container)
        void onMessageContainerClick() {
            Message current = getItemAt(getAdapterPosition());

            if (showingTimeMap.containsKey(current.getId())) {
                time.setVisibility(View.GONE);

                showingTimeMap.remove(current.getId());
            } else {
                time.setVisibility(View.VISIBLE);

                showingTimeMap.put(current.getId(), true);
            }
        }
    }

    public class MessageImageTitleViewHolder extends MessageViewHolder {

        @BindView(R.id.item_message_title)
        TextView title;
        @BindView(R.id.item_message_image)
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
}
