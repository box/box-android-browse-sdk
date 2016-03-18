package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.browse.service.CompletionListener;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import java.io.File;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {

    // The api throws a 400 bad request if the offset is not in multiples of 100
    private static final int DEFAULT_SEARCH_LIMIT = 200;
    private static final String OUT_ITEM = "outItem";

    private static BoxRequestsSearch.Search mRequest;

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param session The BoxSession that should be used to perform network calls.
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxSearchFragment newInstance(BoxSession session, BoxRequestsSearch.Search searchRequest) {
        BoxSearchFragment fragment = new BoxSearchFragment();
        mRequest = searchRequest;
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, session.getUserId());
        args.putSerializable(OUT_ITEM, mRequest);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Helper method creating the simplest search of the entire account for the given query.
     *
     * @param session
     * @param query
     * @return
     */
    public static BoxSearchFragment newInstance(BoxSession session, String query) {
        return BoxSearchFragment.newInstance(session, (new BoxApiSearch(session)).getSearchRequest(query));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getSerializable(OUT_ITEM) instanceof BoxRequestsSearch.Search) {
            mRequest = (BoxRequestsSearch.Search) getArguments().getSerializable(OUT_ITEM);
            setToolbar(mRequest.getQuery());
        }
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsSearch.Search.class.getName());
        return filter;
    }

    public void search(BoxRequestsSearch.Search request) {
        setToolbar(request.getQuery());
        setListItems(new BoxIteratorItems());
        mAdapter.removeAll();
        loadItems();
        mAdapter.notifyDataSetChanged();

    }

    @Override
    protected void loadItems() {
        mRequest.setLimit(DEFAULT_SEARCH_LIMIT)
            .setOffset(0);
        mController.execute(mRequest);
    }

    @Override
    protected void updateItems(BoxIteratorItems items) {
        super.updateItems(items);
        if (items.fullSize() != null && mBoxIteratorItems.size() < items.fullSize()) {
            // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
            BoxRequestsSearch.Search incrementalSearchTask = mRequest
                    .setOffset(mBoxIteratorItems.size())
                    .setLimit(DEFAULT_SEARCH_LIMIT);
            mAdapter.add(new BoxListItem(incrementalSearchTask, BoxRequestsSearch.Search.class.getName()));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mRequest);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void handleResponse(Intent intent) {
        super.handleResponse(intent);
        if (intent.getAction().equals(BoxRequestsSearch.Search.class.getName())) {
            onItemsFetched(intent);
        }
    }

    @Override
    protected void onItemsFetched(Intent intent) {
        super.onItemsFetched(intent);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(CompletionListener.EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    protected void onBindBoxItemViewHolder(BoxItemViewHolder holder) {
        if (holder.getItem() == null || holder.getItem().getBoxItem() == null) {
            return;
        }
        final BoxItem item = holder.getItem().getBoxItem();
        holder.getNameView().setText(item.getName());
        holder.getMetaDescription().setText(BoxSearchListAdapter.createPath(item, File.separator));
        mThumbnailManager.setThumbnailIntoView(holder.getThumbView(), item);
        holder.getProgressBar().setVisibility(View.GONE);
        holder.getMetaDescription().setVisibility(View.VISIBLE);
        holder.getThumbView().setVisibility(View.VISIBLE);
    }
}
