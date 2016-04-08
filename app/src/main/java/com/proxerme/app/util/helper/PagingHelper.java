package com.proxerme.app.util.helper;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.proxerme.library.interfaces.IdItem;

import java.util.List;

/**
 * A helper class for calculating offsets to existing items in a paging based structure.
 *
 * @author Ruben Gees
 */
public class PagingHelper {

    public static final int OFFSET_NOT_CALCULABLE = -2;
    public static final int OFFSET_TOO_LARGE = -1;

    @IntRange(from = OFFSET_NOT_CALCULABLE)
    public static int calculateOffsetFromStart(@NonNull List<? extends IdItem> list,
                                               @NonNull IdItem last,
                                               @IntRange(from = 1) int itemsOnPage) {
        return calculateOffsetFromStart(list, last.getId(), itemsOnPage);
    }

    @IntRange(from = OFFSET_NOT_CALCULABLE)
    public static int calculateOffsetFromEnd(@NonNull List<? extends IdItem> list,
                                             @NonNull IdItem first,
                                             @IntRange(from = 1) int itemsOnPage) {
        return calculateOffsetFromEnd(list, first.getId(), itemsOnPage);
    }

    @IntRange(from = OFFSET_NOT_CALCULABLE)
    public static int calculateOffsetFromStart(@NonNull List<? extends IdItem> list,
                                               @NonNull String id,
                                               @IntRange(from = 1) int itemsOnPage) {
        if (list.isEmpty()) {
            return OFFSET_NOT_CALCULABLE;
        } else {
            for (int i = 0; i < list.size() && i < itemsOnPage; i++) {
                if (id.equals(list.get(i).getId())) {
                    return i;
                }
            }

            return OFFSET_TOO_LARGE;
        }
    }

    @IntRange(from = OFFSET_NOT_CALCULABLE)
    public static int calculateOffsetFromEnd(@NonNull List<? extends IdItem> list,
                                             @NonNull String id,
                                             @IntRange(from = 1) int itemsOnPage) {
        if (list.isEmpty()) {
            return OFFSET_NOT_CALCULABLE;
        } else {
            int lastSearchableIndex = list.size() - itemsOnPage;

            for (int i = list.size() - 1; i >= 0 && i >= lastSearchableIndex; i--) {
                if (id.equals(list.get(i).getId())) {
                    return list.size() - i;
                }
            }

            return OFFSET_TOO_LARGE;
        }
    }

}
