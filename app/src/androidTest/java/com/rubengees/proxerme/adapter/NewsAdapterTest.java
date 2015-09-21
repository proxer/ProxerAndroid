package com.rubengees.proxerme.adapter;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.rubengees.proxerme.activity.DashboardActivity;
import com.rubengees.proxerme.entity.News;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
@RunWith(AndroidJUnit4.class)
public class NewsAdapterTest extends ActivityInstrumentationTestCase2<DashboardActivity> {

    public NewsAdapterTest() {
        super(DashboardActivity.class);
    }

    private News news1;
    private News news2;
    private NewsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        news1 = new News(1, 1, "desc", 1, "subj", 1, 1, 1, "author", 1, 1, "cat");
        news2 = new News(2, 2, "desc", 2, "subj", 2, 2, 2, "author", 2, 2, "cat");

        List<News> list = new ArrayList<>(15);

        for (int i = 0; i < 9; i++) {
            list.add(news2);
        }

        list.add(news1);

        for (int i = 0; i < 5; i++) {
            list.add(news2);
        }

        adapter = new NewsAdapter(list);
    }


    @Test
    public void testInsertAtStart() throws Exception {
        List<News> toInsert = new ArrayList<>(15);

        for (int i = 0; i < 15; i++) {
            toInsert.add(news1);
        }

        assertEquals(10, adapter.insertAtStart(toInsert));
    }

    @Test
    public void testAppend() throws Exception {
        List<News> toAppend = new ArrayList<>(15);

        for (int i = 0; i < 15; i++) {
            toAppend.add(news1);
        }

        assertEquals(6, adapter.append(toAppend));
    }
}