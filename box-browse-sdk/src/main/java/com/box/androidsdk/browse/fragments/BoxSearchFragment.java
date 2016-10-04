package com.box.androidsdk.browse.fragments;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.adapters.BoxSearchAdapter;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {
    private static final String OUT_ITEM = "outItem";
    private static final String OUT_OFFSET = "outOffset";
    private static final int DEFAULT_LIMIT = 200;

    protected static BoxRequestsSearch.Search mRequest;
    protected int mOffset = 0;
    protected int mLimit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getSerializable(OUT_ITEM) instanceof BoxRequestsSearch.Search) {
            mRequest = (BoxRequestsSearch.Search) getArguments().getSerializable(OUT_ITEM);
            mLimit = getArguments().getInt(ARG_LIMIT, DEFAULT_LIMIT);
        }
        if (savedInstanceState != null) {
            mOffset = savedInstanceState.getInt(OUT_OFFSET);
        }
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsSearch.Search.class.getName());
        return filter;
    }

    public void search(BoxRequestsSearch.Search request) {
        mRequest = request;
        mAdapter.removeAll();
        loadItems();
        mAdapter.notifyDataSetChanged();
        notifyUpdateListeners();

    }

    @Override
    protected void loadItems() {
        mOffset = 0;
        mRequest.setLimit(mLimit)
                .setOffset(mOffset);
        getController().execute(mRequest);
    }

    @Override
    protected BoxItemAdapter createAdapter() {
        return new BoxSearchAdapter(getActivity(), getController(), this);
    }

    @Override
    protected void updateItems(ArrayList<BoxItem> items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        mProgress.setVisibility(View.GONE);

        // Search can potentially have a lot of results so incremental loading and de-duping logic is needed
        final int startRange = mAdapter.getItemCount() > 0 ? mAdapter.getItemCount() - 1: 0;

        ArrayList<BoxItem> filteredItems = new ArrayList<BoxItem>();
        ArrayList<BoxItem> adapterItems = mAdapter.getItems();
        HashSet<String> itemIdsInAdapter = new HashSet<String>(adapterItems.size());
        for (BoxItem item : adapterItems){
            itemIdsInAdapter.add(item.getId());
        }
        for (BoxItem item : items) {
            if ((getItemFilter() != null && !getItemFilter().accept(item)) || itemIdsInAdapter.contains(item.getId())) {
                continue;
            }
            filteredItems.add(item);
        }
        if (startRange > 0) {
            mItems.addAll(filteredItems);
            mAdapter.add(filteredItems);
        } else {
            mItems = filteredItems;
            mAdapter.updateTo(mItems);
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mRequest);
        outState.putInt(OUT_OFFSET, mOffset);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void handleResponse(BoxResponseIntent intent) {
        super.handleResponse(intent);
        if (intent.getAction().equals(BoxRequestsSearch.Search.class.getName())) {
            onItemsFetched(intent.getResponse());
        }
    }

    protected void onItemsFetched(BoxResponse response) {
        if (!response.isSuccess()) {
            checkConnectivity();
            return;
        }
        ArrayList<String> removeIds = new ArrayList<String>(1);
        removeIds.add(BoxSearchAdapter.LOAD_MORE_ID);
        mAdapter.remove(removeIds);

        if (response.getResult() instanceof BoxIteratorItems) {
            BoxIteratorItems items = (BoxIteratorItems) response.getResult();
            updateItems(items.getEntries());
            mOffset += items.size();

            // If not all entries were fetched add a task to fetch more items if user scrolls to last entry.
            if (items.fullSize() != null && mOffset < items.fullSize()) {
                // The search endpoint returns a 400 bad request if the offset is not in multiples of the limit
                mOffset = calculateBestOffset(mOffset, mLimit);
                BoxRequestsSearch.Search incrementalSearchTask = mRequest
                        .setOffset(mOffset)
                        .setLimit(mLimit);
                ((BoxSearchAdapter) mAdapter).addLoadMoreItem(incrementalSearchTask);
            }
        }
        mSwipeRefresh.setRefreshing(false);
    }

    /**
     * If for some reason the server returns less than the right number of items
     * for instance if some results are hidden due to permissions offset based off of number of items
     * will not be a multiple of limit.
     * @param itemsSize
     * @param limit
     * @return
     */
    private static int calculateBestOffset(int itemsSize, int limit){
        if (limit % itemsSize == 0){
            return itemsSize;
        }
        int multiple = itemsSize / limit;
        return (multiple + 1) * limit;

    }

    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static class Builder extends BoxBrowseFragment.Builder<BoxSearchFragment> {

        /**
         * @param session
         * @param searchRequest
         */
        public Builder(BoxSession session, BoxRequestsSearch.Search searchRequest) {
            mRequest = searchRequest;
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putSerializable(OUT_ITEM, mRequest);
        }

        /**
         * @param session
         * @param query
         */
        public Builder(BoxSession session, String query) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putSerializable(OUT_ITEM, mRequest);
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
        }


        /**
         * Set the number of items that the results will be limited to when retrieving search results
         *
         * @param limit
         */
        public BoxSearchFragment.Builder setLimit(int limit) {
            mArgs.putInt(ARG_LIMIT, limit);
            return this;
        }

        @Override
        protected BoxSearchFragment getInstance() {
            return new BoxSearchFragment();
        }
    }
}
