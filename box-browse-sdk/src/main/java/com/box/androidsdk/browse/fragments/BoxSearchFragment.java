package com.box.androidsdk.browse.fragments;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.adapters.BoxSearchAdapter;
import com.box.androidsdk.browse.service.BoxBrowseController;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.eclipsesource.json.JsonArray;

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

    private static final String RECENT_SEARCHES_SHARED_PREFERENCES = "com.box.androidsdk.browse.fragments.BoxSearchFragment.RecentSearchesSharedPreferences";
    private static final String RECENT_SEARCHES_KEY = "com.box.androidsdk.browse.fragments.BoxSearchFragment.RecentSearchesKey";
    protected int mOffset = 0;
    protected int mLimit;

    protected String mSearchQuery;
    protected static BoxRequestsSearch.Search mRequest;
    protected ArrayList<String> mRecentSearches;
    protected ArrayAdapter<String> mRecentSearchesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mLimit = getArguments().getInt(ARG_LIMIT, DEFAULT_LIMIT);
        }
        if (savedInstanceState != null) {
            mOffset = savedInstanceState.getInt(OUT_OFFSET);
        }

        mSearchQuery = null;
        mRequest = null;
        mRecentSearches = fetchRecentSearches();
        mRecentSearchesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, mRecentSearches);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (mSearchRecentsListView != null) {
            mSearchRecentsListView.setAdapter(mRecentSearchesAdapter);

            // Show recents only if we are not using an old fragment
            if (mSearchQuery == null) {
                mSearchRecentsListView.setVisibility(View.VISIBLE);
            }
        }
        return view;
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsSearch.Search.class.getName());
        return filter;
    }

    public void search(String query) {
        mSearchQuery = query.trim();
        if (mSearchQuery != null && !mSearchQuery.equals("")) {
            mRequest = mController.getSearchRequest(mSearchQuery);
            mAdapter.removeAll();
            loadItems();
            mAdapter.notifyDataSetChanged();
            notifyUpdateListeners();

            if (mSearchRecentsListView != null) {
                mSearchRecentsListView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void loadItems() {
        if (mRequest != null) {
            mProgress.setVisibility(View.VISIBLE);
            mOffset = 0;
            mRequest.setLimit(mLimit)
                    .setOffset(mOffset);
            getController().execute(mRequest);
        }
    }

    @Override
    public void onItemClick(BoxItem item) {
        if (mSearchQuery != null && !mSearchQuery.equals("")) {
            addToRecentSearches(mSearchQuery);
        }
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
         */
        public Builder(BoxSession session) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
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

    protected void addToRecentSearches(String recent) {
        mRecentSearches.remove(recent);
        if (mRecentSearches.size() >= 50) {
            mRecentSearches.remove(49);
        }
        mRecentSearches.add(0, recent);
        saveRecentSearches(mRecentSearches);
    }

    protected ArrayList<String> fetchRecentSearches() {
        String string = getContext().getSharedPreferences(RECENT_SEARCHES_SHARED_PREFERENCES, Context.MODE_PRIVATE).getString(RECENT_SEARCHES_KEY, null);
        ArrayList<String> recentSearches = new ArrayList<String>();

        if (string != null) {
            JsonArray jsonArray = JsonArray.readFrom(string);
            for (int i = 0; i < jsonArray.size(); i++) {
                recentSearches.add(jsonArray.get(i).asString());
            }
        }

        return recentSearches;
    }

    protected void saveRecentSearches(ArrayList<String> searches) {
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < searches.size(); i++) {
            jsonArray.add(searches.get(i));
        }
        getContext().getSharedPreferences(RECENT_SEARCHES_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putString(RECENT_SEARCHES_KEY, jsonArray.toString()).commit();
    }
}
