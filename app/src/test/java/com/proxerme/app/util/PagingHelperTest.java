package com.proxerme.app.util;

import com.proxerme.library.entity.Conference;
import com.proxerme.library.interfaces.IdItem;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class PagingHelperTest {

    private static final int ITEMS_ON_PAGE = 15;
    private List<IdItem> list;

    @Before
    public void setUp() throws Exception {
        list = new ArrayList<>(15);

        for (int i = 0; i < 8; i++) {
            list.add(new Conference(String.valueOf(i), "a", 2, false, 324, false, "a"));
        }

        for (int i = 14; i >= 8; i--) {
            list.add(new Conference(String.valueOf(i), "a", 2, false, 324, false, "a"));
        }
    }

    @Test
    public void testCalculateOffsetFromStart() throws Exception {
        IdItem first = new Conference("3", "a", 2, false, 324, false, "a");

        Assert.assertEquals(3, PagingHelper.calculateOffsetFromStart(list, first, ITEMS_ON_PAGE));
    }

    @Test
    public void testCalculateOffsetFromEnd() throws Exception {
        IdItem last = new Conference("3", "a", 2, false, 324, false, "a");

        Assert.assertEquals(11, PagingHelper.calculateOffsetFromEnd(list, last, ITEMS_ON_PAGE));
    }

    @Test
    public void testCalculateOffsetFromStartId() throws Exception {
        Assert.assertEquals(3, PagingHelper.calculateOffsetFromStart(list, "3", ITEMS_ON_PAGE));
    }

    @Test
    public void testCalculateOffsetFromEndId() throws Exception {
        Assert.assertEquals(11, PagingHelper.calculateOffsetFromEnd(list, "3", ITEMS_ON_PAGE));
    }
}