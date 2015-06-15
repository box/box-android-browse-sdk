package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {


    private static final String OUT_ITEM = "outItem";
    BoxSearchHolder mSearchRequestHolder;
    protected BoxApiSearch mApiSearch;
    protected static final int DEFAULT_SEARCH_LIMIT = 30;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getSerializable(OUT_ITEM) instanceof BoxSearchHolder) {
            mSearchRequestHolder = (BoxSearchHolder) getArguments().getSerializable(OUT_ITEM);
            setToolbar(mSearchRequestHolder.getQuery());
        }
        mApiSearch = new BoxApiSearch(mSession);
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param session The BoxSession that should be used to perform network calls.
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxSearchFragment newInstance(BoxSession session, BoxRequestsSearch.Search searchRequest) {
        BoxSearchFragment fragment = new BoxSearchFragment();
        BoxSearchHolder holder = new BoxSearchHolder(searchRequest);
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, session.getUserId());
        args.putSerializable(OUT_ITEM, holder);
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


    public void search(BoxRequestsSearch.Search request){
        mSearchRequestHolder = new BoxSearchHolder(request);
        setListItem(new BoxListItems());
        mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public FutureTask<Intent> fetchInfo() {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_INFO);
                try {
                    BoxListItems items = mSearchRequestHolder.createSearchRequest(mApiSearch).setLimit(DEFAULT_SEARCH_LIMIT).send();
                    if (items != null) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                        intent.putExtra(EXTRA_COLLECTION, items);
                    }

                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    public FutureTask<Intent> fetchItems(final int offset, final int limit) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_ITEMS);
                intent.putExtra(EXTRA_OFFSET, offset);
                intent.putExtra(EXTRA_LIMIT, limit);
                try {
                    BoxRequestsSearch.Search search = mSearchRequestHolder.createSearchRequest(mApiSearch).setOffset(offset).setLimit(limit);
                    BoxConfig.IS_DEBUG = true;
                    BoxConfig.IS_LOG_ENABLED = true;
                    int limitUsed = limit;
                    // according to the https://box-content.readme.io/#searching-for-content  offset must be a multiple of limit for simplicity we use the offset itself.
                    if (offset % limit != 0 && offset != 0){
                        limitUsed = offset;
                    }

                    BoxListItems items = mSearchRequestHolder.createSearchRequest(mApiSearch).setOffset(offset).setLimit(limitUsed).send();
                    intent.putExtra(EXTRA_SUCCESS, true);
                    intent.putExtra(EXTRA_COLLECTION, items);
                } catch (BoxException e) {
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mSearchRequestHolder);
        super.onSaveInstanceState(outState);
    }


    protected void onItemsFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
            return;
        }
        super.onItemsFetched(intent);
    }

    protected void onInfoFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null || mAdapter == null) {
            return;
        }

        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
            return;
        }
        super.onInfoFetched(intent);
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

    /**
     * Helper class to hold onto the parameters held by given search request. This class is not meant
     * to be used beyond storing and retrieving these fields.
     */
    public static class BoxSearchHolder extends BoxRequestsSearch.Search implements Serializable {
        /**
         * Construct a search holder.
         *
         * @param searchRequest the search request used to populate this holder.
         */
        public BoxSearchHolder(BoxRequestsSearch.Search searchRequest) {
            super("", "", null);
            importRequestContentMapsFrom(searchRequest);
        }

        /**
         * Create a new search request from the given search holder object.
         *
         * @param searchApi the search api that should be used for this request.
         * @return a new search based off the parameters of this holder.
         */
        public BoxRequestsSearch.Search createSearchRequest(final BoxApiSearch searchApi) {
            BoxRequestsSearch.Search search = searchApi.getSearchRequest(this.getQuery());
            for (Map.Entry<String, String> entry : mQueryMap.entrySet()) {
                search.limitValueForKey(entry.getKey(), entry.getValue());
            }
            return search;
        }
    }
}
