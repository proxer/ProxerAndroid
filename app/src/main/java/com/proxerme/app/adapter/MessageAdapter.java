package com.proxerme.app.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.utils.Utils;
import com.proxerme.app.R;
import com.proxerme.app.util.TimeUtils;
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
import butterknife.OnLongClick;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MessageAdapter extends PagingAdapter<Message, MessageAdapter.MessageViewHolder> {

    private static final String STATE_MESSAGE_SELECTED_IDS = "message_selected_ids";
    private static final String STATE_MESSAGE_SELECTING = "message_selecting";
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

    private HashMap<String, Boolean> selectedMap;
    private HashMap<String, Boolean> showingTimeMap;

    private boolean selecting = false;

    private OnMessageInteractionListener onMessageInteractionListener;

    public MessageAdapter(@Nullable LoginUser user) {
        this.user = user;
        this.selectedMap = new HashMap<>(ProxerInfo.MESSAGES_ON_PAGE * 2);
        this.showingTimeMap = new HashMap<>(ProxerInfo.MESSAGES_ON_PAGE * 2);
    }

    public MessageAdapter(@NonNull Collection<Message> list, @Nullable LoginUser user) {
        super(list);

        this.user = user;
        this.selectedMap = new HashMap<>(list.size() * 2);
        this.showingTimeMap = new HashMap<>(list.size() * 2);
    }

    public MessageAdapter(@NonNull Bundle savedInstanceState, @Nullable LoginUser user) {
        super(savedInstanceState);

        this.user = user;

        List<String> selectedIds = savedInstanceState.getStringArrayList(STATE_MESSAGE_SELECTED_IDS);
        List<String> showingTimeIds =
                savedInstanceState.getStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS);

        this.selecting = savedInstanceState.getBoolean(STATE_MESSAGE_SELECTING);
        this.selectedMap = new HashMap<>();
        this.showingTimeMap = new HashMap<>();

        if (selectedIds != null) {
            for (String id : selectedIds) {
                this.selectedMap.put(id, true);
            }
        }

        if (showingTimeIds != null) {
            for (String id : showingTimeIds) {
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

        if (viewType == TYPE_ACTION) {
            return new ActionViewHolder(inflater.inflate(R.layout.item_message_action,
                    parent, false));
        } else if (viewType == TYPE_MESSAGE_TOP || viewType == TYPE_MESSAGE_SINGLE) {
            return new MessageImageTitleViewHolder(inflater.inflate(R.layout.item_message_single,
                    parent, false));
        } else if (viewType == TYPE_MESSAGE_BOTTOM || viewType == TYPE_MESSAGE_INNER) {
            return new MessageViewHolder(inflater.inflate(R.layout.item_message,
                    parent, false));
        } else {
            return new MessageViewHolder(inflater.inflate(R.layout.item_message_self,
                    parent, false));
        }
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Pair<Integer, Integer> margins = getMarginsForPosition(position);

        holder.bind(getItemAt(position),
                Utils.convertDpToPx(holder.container.getContext(), margins.first),
                Utils.convertDpToPx(holder.container.getContext(), margins.second));
    }

    private Pair<Integer, Integer> getMarginsForPosition(int position) {
        int marginTop;
        int marginBottom;
        int viewType = getItemViewType(position);

        switch (viewType) {
            case TYPE_MESSAGE_INNER:
                marginTop = 2;
                marginBottom = 2;

                break;
            case TYPE_MESSAGE_SINGLE:
                marginTop = 8;
                marginBottom = 8;

                break;
            case TYPE_MESSAGE_TOP:
                marginTop = 8;
                marginBottom = 2;

                break;
            case TYPE_MESSAGE_BOTTOM:
                marginTop = 2;
                marginBottom = 8;

                break;
            case TYPE_MESSAGE_SELF_INNER:
                marginTop = 2;
                marginBottom = 2;

                break;
            case TYPE_MESSAGE_SELF_SINGLE:
                marginTop = 8;
                marginBottom = 8;

                break;
            case TYPE_MESSAGE_SELF_TOP:
                marginTop = 8;
                marginBottom = 2;

                break;
            case TYPE_MESSAGE_SELF_BOTTOM:
                marginTop = 2;
                marginBottom = 8;

                break;
            case TYPE_ACTION:
                marginTop = 16;
                marginBottom = 16;

                break;
            default:
                throw new RuntimeException("An unknown viewType was passed: " + viewType);
        }

        return new Pair<>(marginTop, marginBottom);
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
    public void saveInstanceState(@NonNull Bundle outState) {
        super.saveInstanceState(outState);

        outState.putBoolean(STATE_MESSAGE_SELECTING, selecting);
        outState.putStringArrayList(STATE_MESSAGE_SELECTED_IDS,
                new ArrayList<>(selectedMap.keySet()));
        outState.putStringArrayList(STATE_MESSAGE_SHOWING_TIME_IDS,
                new ArrayList<>(showingTimeMap.keySet()));
    }

    @Override
    public void clear() {
        super.clear();

        user = null;
    }

    @NonNull
    public List<Message> getSelectedItems() {
        List<Message> result = new ArrayList<>(selectedMap.size());

        for (int i = 0; i < getItemCount(); i++) {
            if (selectedMap.containsKey(getItemAt(i).getId())) {
                result.add(getItemAt(i));
            }
        }

        return result;
    }

    public boolean handleBackPress() {
        if (selecting) {
            selecting = false;

            selectedMap.clear();
            notifyDataSetChanged();

            return true;
        } else {
            return false;
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

        public void onMessageSelection(int count) {

        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_message_container)
        ViewGroup container;
        @BindView(R.id.item_message_message)
        TextView text;
        @BindView(R.id.item_message_time)
        TextView time;

        public MessageViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public void bind(@NonNull Message message, int marginTop, int marginBottom) {
            text.setText(message.getMessage());
            time.setText(TimeUtils.convertToRelativeReadableTime(time.getContext(),
                    message.getTime()));

            Linkify.addLinks(text, Linkify.ALL);

            if (selectedMap.containsKey(message.getId())) {
                container.setBackgroundResource(R.color.md_grey_200);
            } else {
                container.setBackgroundResource(android.R.color.white);
            }

            if (showingTimeMap.containsKey(message.getId())) {
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) container.getLayoutParams();

            params.topMargin = marginTop;
            params.bottomMargin = marginBottom;

            container.setLayoutParams(params);
        }

        @OnClick(R.id.item_message_container)
        void onMessageContainerClick() {
            Message current = getItemAt(getAdapterPosition());

            if (selecting) {
                if (selectedMap.containsKey(current.getId())) {
                    selectedMap.remove(current.getId());

                    if (selectedMap.size() <= 0) {
                        selecting = false;
                    }
                } else {
                    selectedMap.put(current.getId(), true);
                }

                if (onMessageInteractionListener != null) {
                    onMessageInteractionListener.onMessageSelection(selectedMap.size());
                }
            } else {
                if (showingTimeMap.containsKey(current.getId())) {
                    showingTimeMap.remove(current.getId());
                } else {
                    showingTimeMap.put(current.getId(), true);
                }
            }

            notifyDataSetChanged();
        }

        @OnLongClick(R.id.item_message_container)
        boolean onMessageContainerLongClick() {
            Message current = getItemAt(getAdapterPosition());

            if (!selecting) {
                selecting = true;

                selectedMap.put(current.getId(), true);
                notifyDataSetChanged();

                if (onMessageInteractionListener != null) {
                    onMessageInteractionListener.onMessageSelection(selectedMap.size());
                }

                return true;
            }

            return false;
        }
    }

    public class MessageImageTitleViewHolder extends MessageViewHolder {

        @BindView(R.id.item_message_title)
        TextView title;
        @BindView(R.id.item_message_image)
        ImageView image;

        public MessageImageTitleViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(@NonNull Message message, int marginTop, int marginBottom) {
            super.bind(message, marginTop, marginBottom);

            title.setText(message.getUsername());

            if (TextUtils.isEmpty(message.getImageId())) {
                image.setImageDrawable(new IconicsDrawable(image.getContext())
                        .icon(CommunityMaterial.Icon.cmd_account).sizeDp(32).paddingDp(32)
                        .colorRes(R.color.colorPrimary));
            } else {
                Glide.with(image.getContext())
                        .load(UrlHolder.getUserImageUrl(message.getImageId()))
                        .into(image);
            }
        }

        @OnClick(R.id.item_message_image)
        public void onImageClick(View v) {
            if (onMessageInteractionListener != null) {
                onMessageInteractionListener.onMessageImageClick(v,
                        getItemAt(getAdapterPosition()));
            }
        }
    }

    public class ActionViewHolder extends MessageViewHolder {

        public ActionViewHolder(View itemView) {
            super(itemView);
        }

        @Override
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
}
