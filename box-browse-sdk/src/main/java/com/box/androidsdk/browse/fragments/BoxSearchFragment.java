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

import java.util.ArrayList;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {
    private static final String OUT_ITEM = "outItem";

    private static BoxRequestsSearch.Search mRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getSerializable(OUT_ITEM) instanceof BoxRequestsSearch.Search) {
            mRequest = (BoxRequestsSearch.Search) getArguments().getSerializable(OUT_ITEM);
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
        mRequest.setLimit(mLimit)
                .setOffset(0);
        getController().execute(mRequest);
    }

    @Override
    protected BoxItemAdapter createAdapter() {
        return new BoxSearchAdapter(getActivity(), null, getController(), this);
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
        for (BoxItem item : items) {
            if ((getItemFilter() != null && !getItemFilter().accept(item)) || mAdapter.contains(item)) {
                continue;
            }
            filteredItems.add(item);
        }
        if (startRange > 0) {
            mAdapter.addAll(filteredItems);
        } else {
            mItems = filteredItems;
            mAdapter.setItems(mItems);
        }

        final int endRange = mAdapter.getItemCount();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeChanged(startRange, endRange);
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mRequest);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void handleResponse(BoxResponseIntent intent) {
        super.handleResponse(intent);
        if (intent.getAction().equals(BoxRequestsSearch.Search.class.getName())) {
            onItemsFetched(intent);
        }
    }

    private void onItemsFetched(BoxResponseIntent intent) {
//        // On failure updates the existing loading item with an error item
//        if (!intent.isSuccess()) {
//            checkConnectivity();
//            BoxListItem item = mAdapter.get(intent.getAction());
//            if (item != null) {
//                item.setResponse(intent);
//                item.setState(BoxListItem.State.ERROR);
//                mAdapter.update(intent.getAction());
//            }
//            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // On success should remove the existing loading item
//        mAdapter.remove(intent.getAction());
        if (intent.getResult() instanceof BoxIteratorItems) {
            ArrayList<BoxItem> items = ((BoxIteratorItems) intent.getResult()).getEntries();
            updateItems(items);
        }
        mSwipeRefresh.setRefreshing(false);
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
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);

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

        @Override
        protected BoxSearchFragment getInstance() {
            return new BoxSearchFragment();
        }
    }
}
