package adapter;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class FooterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOOTER = Integer.MIN_VALUE;
    private static final long ID_FOOTER = Long.MIN_VALUE;

    private RecyclerView.Adapter innerAdapter;
    private View footer;

    private RecyclerView.LayoutManager layoutManager;

    public FooterAdapter(RecyclerView.Adapter innerAdapter) {
        this.innerAdapter = innerAdapter;

        this.innerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                notifyDataSetChanged();
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart, itemCount);
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                notifyItemMoved(fromPosition, toPosition);
            }
        });

        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        layoutManager = recyclerView.getLayoutManager();

        initLayoutManager();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER && footer != null) {
            if (layoutManager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager.LayoutParams layoutParams;

                if (footer.getLayoutParams() == null) {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                } else {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(
                            footer.getLayoutParams().width,
                            footer.getLayoutParams().height
                    );
                }

                layoutParams.setFullSpan(true);

                footer.setLayoutParams(layoutParams);
            }

            return new FooterViewHolder(footer);
        } else {
            return innerAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof FooterViewHolder)) {
            //noinspection unchecked
            innerAdapter.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return innerAdapter.getItemCount() + (footer == null ? 0 : 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooter(position)) {
            return TYPE_FOOTER;
        } else {
            return innerAdapter.getItemViewType(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if (isFooter(position)) {
            return ID_FOOTER;
        } else {
            return innerAdapter.getItemId(position);
        }
    }

    public boolean isFooter(int position) {
        return footer != null && position == innerAdapter.getItemCount();
    }

    public void setFooter(View footer) {
        if (footer == null) {
            this.footer = null;
        } else {
            this.footer = footer;
        }

        notifyDataSetChanged();
    }

    public void removeFooter() {
        this.footer = null;

        notifyDataSetChanged();
    }

    public RecyclerView.Adapter getInnerAdapter() {
        return innerAdapter;
    }

    private void initLayoutManager() {
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager castedLayoutManager = (GridLayoutManager) layoutManager;
            final SpanSizeLookup existingLookup = castedLayoutManager.getSpanSizeLookup();

            castedLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (isFooter(position)) {
                        return castedLayoutManager.getSpanCount();
                    }

                    return existingLookup.getSpanSize(position);
                }
            });
        }
    }

    private static class FooterViewHolder extends RecyclerView.ViewHolder {

        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
