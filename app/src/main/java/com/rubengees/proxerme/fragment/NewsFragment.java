package com.rubengees.proxerme.fragment;


import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.adapter.NewsAdapter;
import com.rubengees.proxerme.connection.ProxerConnection;
import com.rubengees.proxerme.connection.ProxerException;
import com.rubengees.proxerme.entity.News;
import com.rubengees.proxerme.util.SnackbarManager;

import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsFragment extends Fragment {

    private RecyclerView list;

    private NewsAdapter adapter;
    private SwipeRefreshLayout root;

    public static NewsFragment newInstance() {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    public NewsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            adapter = new NewsAdapter();
            loadNews(1);
        } else {
            adapter = new NewsAdapter(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_news, container, false);
        list = (RecyclerView) root.findViewById(R.id.fragment_news_list);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1,
                StaggeredGridLayoutManager.VERTICAL);

        list.setHasFixedSize(true);
        list.setAdapter(adapter);
        list.setLayoutManager(layoutManager);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
    }

    private void loadNews(@IntRange(from = 1) final int page) {
        ProxerConnection.loadNews(page, new ProxerConnection.ResultCallback<List<News>>() {
            @Override
            public void onResult(List<News> result) {
                handleResult(page, result);
            }

            @Override
            public void onError(ProxerException exception) {
                SnackbarManager.show(Snackbar.make(root, exception.getMessage(),
                        Snackbar.LENGTH_INDEFINITE), "Retry", new SnackbarManager.SnackbarCallback() {
                    @Override
                    public void onClick(View v) {
                        //TODO
                    }
                });
            }
        });
    }

    private void handleResult(int page, List<News> result) {
        adapter.insertAtStart(result);
    }

}
