package adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.proxerme.app.R;

import java.util.List;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class HeaderFooterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = Integer.MIN_VALUE;
    private static final int TYPE_FOOTER = Integer.MIN_VALUE + 1;
    private static final long ID_HEADER = Long.MIN_VALUE;
    private static final long ID_FOOTER = Long.MIN_VALUE + 1;

    private RecyclerView.Adapter innerAdapter;

    private View header;
    private View footer;

    private RecyclerView.LayoutManager layoutManager;

    public HeaderFooterAdapter(RecyclerView.Adapter innerAdapter) {
        this.innerAdapter = innerAdapter;

        this.innerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                notifyDataSetChanged();
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(getDelegatedPosition(positionStart), itemCount);
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                notifyItemRangeChanged(getDelegatedPosition(positionStart), itemCount, payload);
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(getDelegatedPosition(positionStart), itemCount);
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(getDelegatedPosition(positionStart), itemCount);
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                notifyItemRangeChanged(getDelegatedPosition(fromPosition),
                        getDelegatedPosition(toPosition) + itemCount);
            }
        });

        setHasStableIds(innerAdapter.hasStableIds());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        layoutManager = recyclerView.getLayoutManager();

        initLayoutManager();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER || viewType == TYPE_FOOTER) {
            return new HeaderFooterViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.adapter_header_footer_item, parent, false));
        } else {
            return innerAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position,
                                 List<Object> payloads) {
        if (holder instanceof HeaderFooterViewHolder) {
            bind((HeaderFooterViewHolder) holder, position);
        } else {
            //noinspection unchecked
            innerAdapter.onBindViewHolder(holder, getPositionForInnerAdapter(position), payloads);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderFooterViewHolder) {
            bind((HeaderFooterViewHolder) holder, position);
        } else {
            //noinspection unchecked
            innerAdapter.onBindViewHolder(holder, getPositionForInnerAdapter(position));
        }
    }

    @Override
    public int getItemCount() {
        return innerAdapter.getItemCount() + (hasHeader() ? 1 : 0) + (hasFooter() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return TYPE_HEADER;
        } else if (isFooter(position)) {
            return TYPE_FOOTER;
        } else {
            return innerAdapter.getItemViewType(getPositionForInnerAdapter(position));
        }
    }

    @Override
    public long getItemId(int position) {
        if (isHeader(position)) {
            return ID_HEADER;
        } else if (isFooter(position)) {
            return ID_FOOTER;
        } else {
            return innerAdapter.getItemId(getPositionForInnerAdapter(position));
        }
    }

    public boolean hasHeader() {
        return header != null;
    }

    public boolean hasFooter() {
        return footer != null;
    }

    public boolean isHeader(int position) {
        return header != null && position == 0;
    }

    public boolean isFooter(int position) {
        return footer != null && position == getFooterPosition();
    }

    public void setHeader(@Nullable View header) {
        boolean hadHeader = this.header != null;
        this.header = header;

        if (header == null) {
            if (hadHeader) {
                notifyItemRemoved(0);
            }
        } else {
            if (hadHeader) {
                notifyItemChanged(0);
            } else {
                notifyItemInserted(0);
            }
        }
    }

    public void setFooter(@Nullable View footer) {
        boolean hadFooter = this.footer != null;
        this.footer = footer;

        if (footer == null) {
            if (hadFooter) {
                notifyItemRemoved(getFooterPosition());
            }
        } else {
            if (hadFooter) {
                notifyItemChanged(getFooterPosition());
            } else {
                notifyItemInserted(getFooterPosition());
            }
        }
    }

    public void removeHeader() {
        setHeader(null);
    }

    public void removeFooter() {
        setFooter(null);
    }

    public RecyclerView.Adapter getInnerAdapter() {
        return innerAdapter;
    }

    private int getDelegatedPosition(int position) {
        return position + (hasHeader() ? 1 : 0);
    }

    private int getPositionForInnerAdapter(int position) {
        return position - (hasHeader() ? 1 : 0);
    }

    private int getFooterPosition() {
        return innerAdapter.getItemCount() + (hasHeader() ? 1 : 0);
    }

    private void initLayoutManager() {
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager castedLayoutManager = (GridLayoutManager) layoutManager;
            final SpanSizeLookup existingLookup = castedLayoutManager.getSpanSizeLookup();

            castedLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (isHeader(position) || isFooter(position)) {
                        return castedLayoutManager.getSpanCount();
                    }

                    return existingLookup.getSpanSize(position);
                }
            });
        }
    }

    private void bind(HeaderFooterViewHolder holder, int position) {
        View viewToAdd = isHeader(position) ? header : footer;

        ((ViewGroup) holder.itemView).removeAllViews();
        ((ViewGroup) holder.itemView).addView(viewToAdd);

        ViewGroup.LayoutParams layoutParams;

        if (layoutManager instanceof StaggeredGridLayoutManager) {
            if (viewToAdd.getLayoutParams() == null) {
                layoutParams = new StaggeredGridLayoutManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            } else {
                layoutParams = new StaggeredGridLayoutManager.LayoutParams(
                        viewToAdd.getLayoutParams().width,
                        viewToAdd.getLayoutParams().height
                );
            }

            ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
        } else {
            if (viewToAdd.getLayoutParams() == null) {
                layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            } else {
                layoutParams = new ViewGroup.LayoutParams(
                        viewToAdd.getLayoutParams().width,
                        viewToAdd.getLayoutParams().height
                );
            }
        }

        holder.itemView.setLayoutParams(layoutParams);
    }

    private static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {

        public HeaderFooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
