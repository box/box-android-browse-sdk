package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.activities.BoxBrowseActivity;
import com.box.androidsdk.browse.activities.FilterSearchResults;
import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.adapters.BoxRecentSearchAdapter;
import com.box.androidsdk.browse.adapters.BoxSearchAdapter;
import com.box.androidsdk.browse.adapters.ResultsHeader;
import com.box.androidsdk.browse.models.BoxSearchFilters;
import com.box.androidsdk.browse.service.BoxBrowseController;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.eclipsesource.json.JsonArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment implements BoxRecentSearchAdapter.BoxRecentSearchListener {

    private static String EXTRA_PARENT_FOLDER = "SearchFragment.ExtraParentFolder";
    private static final String OUT_ITEM = "outItem";
    private static final String OUT_OFFSET = "outOffset";
    private static final String OUT_QUERY = "outQuery";
    private static final int DEFAULT_LIMIT = 200;

    public static final String RECENT_SEARCHES_SHARED_PREFERENCES = "com.box.androidsdk.browse.fragments.BoxSearchFragment.RecentSearchesSharedPreferences";
    public static final String RECENT_SEARCHES_KEY = "com.box.androidsdk.browse.fragments.BoxSearchFragment.RecentSearchesKey";
    protected int mOffset = 0;
    protected int mLimit;

    protected String mSearchQuery;
    protected static BoxRequestsSearch.Search mRequest;
    protected ArrayList<String> mRecentSearches;
    protected BoxRecentSearchAdapter mRecentSearchesAdapter;

    public static final int REQUEST_FILTER_SEARCH_RESULTS = 228;
    public static final String EXTRA_SEARCH_FILTERS = "SearchFragment.SearchFilters";

    private View mSearchFiltersHeader;
    protected BoxSearchFilters mSearchFilters;
    private HashMap<BoxSearchFilters.ItemType, String[]> mItemTypeToExtensions;

    // Size of 1MB as per search API
    private final static long ONE_MB = 1000000;

    protected BoxFolder mParentFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchQuery = null;
        if (getArguments() != null) {
            mLimit = getArguments().getInt(ARG_LIMIT, DEFAULT_LIMIT);
            mSearchQuery = getArguments().getString(OUT_QUERY, null);
            mParentFolder = (BoxFolder) getArguments().getSerializable(EXTRA_PARENT_FOLDER);
        }
        if (savedInstanceState != null) {
            mOffset = savedInstanceState.getInt(OUT_OFFSET);
            mSearchQuery = savedInstanceState.getString(OUT_QUERY, null);
            mSearchFilters = (BoxSearchFilters) savedInstanceState.getSerializable(EXTRA_SEARCH_FILTERS);
        }

        if (mSearchFilters == null) {
            mSearchFilters = new BoxSearchFilters();
        }

        mItemTypeToExtensions = new HashMap<BoxSearchFilters.ItemType, String[]>();
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Audio, ThumbnailManager.AUDIO_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.BoxNote, ThumbnailManager.BOXNOTE_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Document, ThumbnailManager.DOCUMENTS_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Image, ThumbnailManager.IMAGE_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Pdf, ThumbnailManager.PDF_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Presentation, ThumbnailManager.PRESENTATION_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Spreadsheet, ThumbnailManager.SPREADSHEET_EXTENSIONS_ARRAY);
        mItemTypeToExtensions.put(BoxSearchFilters.ItemType.Video, ThumbnailManager.VIDEO_EXTENSIONS_ARRAY);

        mRequest = null;
        mRecentSearches = fetchRecentSearches();
        mRecentSearchesAdapter = new BoxRecentSearchAdapter(getActivity(), mRecentSearches, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (mSearchRecentsListView != null) {
            mSearchRecentsListView.setAdapter(mRecentSearchesAdapter);

            // Show recents only if we are not using an old fragment
            if (mSearchQuery == null) {
                mSearchRecentsListView.setVisibility(View.VISIBLE);
                mSearchRecentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String query = mRecentSearches.get(position);
                        Activity activity = getActivity();
                        if (activity instanceof BoxBrowseActivity) {
                            ((BoxBrowseActivity)activity).setSearchQuery(query);
                        }
                    }
                });
            }
        }

        mSearchFiltersHeader = view.findViewById(R.id.filterResultsHeader);
        mSearchFiltersHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFilterActivity();
            }
        });
        setupSearchFiltersHeader();

        return view;
    }

    protected void startFilterActivity() {
        startActivityForResult(FilterSearchResults.newFilterSearchResultsIntent(getActivity(), mSearchFilters), REQUEST_FILTER_SEARCH_RESULTS);
    }

    private void setupSearchFiltersHeader() {
        if (mSearchFiltersHeader == null || mSearchFilters == null) {
            return;
        }

        if (mSearchFilters.anyFiltersSet()) {
            mSearchFiltersHeader.findViewById(R.id.filterResultsHeaderUnset).setVisibility(View.GONE);
            mSearchFiltersHeader.findViewById(R.id.filterResultsHeaderSet).setVisibility(View.VISIBLE);

            TextView textView = (TextView) mSearchFiltersHeader.findViewById(R.id.filterResults);
            ArrayList<String> filters = new ArrayList<String>();
            for (BoxSearchFilters.ItemType type: BoxSearchFilters.ItemType.values()) {
                if (mSearchFilters.mItemTypes.contains(type)) {
                    switch (type) {
                        case Audio:
                            filters.add(getResources().getString(R.string.search_filter_file_type_audio));
                            break;
                        case BoxNote:
                            filters.add(getResources().getString(R.string.search_filter_file_type_boxnote));
                            break;
                        case Document:
                            filters.add(getResources().getString(R.string.search_filter_file_type_document));
                            break;
                        case Folder:
                            filters.add(getResources().getString(R.string.search_filter_file_type_folder));
                            break;
                        case Image:
                            filters.add(getResources().getString(R.string.search_filter_file_type_image));
                            break;
                        case Pdf:
                            filters.add(getResources().getString(R.string.search_filter_file_type_pdf));
                            break;
                        case Presentation:
                            filters.add(getResources().getString(R.string.search_filter_file_type_presentation));
                            break;
                        case Spreadsheet:
                            filters.add(getResources().getString(R.string.search_filter_file_type_spreadsheet));
                            break;
                        case Video:
                            filters.add(getResources().getString(R.string.search_filter_file_type_video));
                            break;
                    }
                }
            }

            switch (mSearchFilters.mItemModifiedDate) {
                case Any:
                    filters.add(getResources().getString(R.string.any_time));
                    break;
                case PastDay:
                    filters.add(getResources().getString(R.string.past_day));
                    break;
                case PastWeek:
                    filters.add(getResources().getString(R.string.past_week));
                    break;
                case PastMonth:
                    filters.add(getResources().getString(R.string.past_month));
                    break;
                case PastYear:
                    filters.add(getResources().getString(R.string.past_year));
                    break;
            }

            switch (mSearchFilters.mItemSize) {
                case Any:
                    filters.add(getResources().getString(R.string.item_size_any));
                    break;
                case lessThanOneMb:
                    filters.add(getResources().getString(R.string.item_size_0_to_1));
                    break;
                case OneMbToFiveMb:
                    filters.add(getResources().getString(R.string.item_size_1_to_5));
                    break;
                case FiveMbToTwentyFiveMb:
                    filters.add(getResources().getString(R.string.item_size_5_to_25));
                    break;
                case TwentyFiveMbToHundredMb:
                    filters.add(getResources().getString(R.string.item_size_25_to_100));
                    break;
                case HundredMbToOneGB:
                    filters.add(getResources().getString(R.string.item_size_100_to_1000));
                    break;
            }

            String label = android.text.TextUtils.join(getResources().getString(R.string.search_filter_label_delimiter), filters);
            textView.setText(label);
        } else {
            mSearchFiltersHeader.findViewById(R.id.filterResultsHeaderUnset).setVisibility(View.VISIBLE);
            mSearchFiltersHeader.findViewById(R.id.filterResultsHeaderSet).setVisibility(View.GONE);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK){
            return;
        }

        if (requestCode == REQUEST_FILTER_SEARCH_RESULTS) {
            mSearchFilters = (BoxSearchFilters) data.getSerializableExtra(EXTRA_SEARCH_FILTERS);
            setupSearchFiltersHeader();
            search();
        }
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsSearch.Search.class.getName());
        return filter;
    }

    public String getSearchQuery() {
        if (mSearchQuery != null) {
            return mSearchQuery;
        }

        return "";
    }

    public void search(String query) {
        if (query != null) {
            String trimmedQuery = query.trim();
            if (!trimmedQuery.equals(mSearchQuery) || mRequest == null) {
                mSearchQuery = trimmedQuery;
                search();
            }
        }
    }

    protected void search() {
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

    protected void executeRequest() {
        getController().execute(mRequest);
    }

    @Override
    protected void loadItems() {
        if (mRequest != null) {
            mProgress.setVisibility(View.VISIBLE);
            mSearchFiltersHeader.setVisibility(View.GONE);
            mOffset = 0;
            mRequest.setLimit(mLimit)
                    .setOffset(mOffset)
                    .limitAncestorFolderIds(new String[]{mParentFolder.getId()});

            // Set filters
            if (mSearchFilters != null) {
                HashSet<BoxSearchFilters.ItemType> itemTypes = mSearchFilters.mItemTypes;
                if (itemTypes != null && itemTypes.size() > 0) {
                    // We need to add type filter
                    if (itemTypes.contains(BoxSearchFilters.ItemType.Folder)) {
                        // Set only folder
                        mRequest.limitType(BoxFolder.TYPE);
                    } else {
                        // Set extension types
                        mRequest.limitType(BoxFile.TYPE);

                        HashSet<String> extensions = new HashSet<String>();
                        for(BoxSearchFilters.ItemType type : itemTypes) {
                            for (String extension : mItemTypeToExtensions.get(type)) {
                                if (!extensions.contains(extension)) {
                                    extensions.add(extension);
                                }
                            }
                        }
                        mRequest.limitFileExtensions(extensions.toArray(new String[extensions.size()]));
                    }
                }

                if (mSearchFilters.mItemModifiedDate != BoxSearchFilters.ItemModifiedDate.Any) {
                    // Add filter for modified date
                    Calendar cal = Calendar.getInstance();

                    switch (mSearchFilters.mItemModifiedDate) {
                        case PastDay:
                            cal.add(Calendar.DATE, -1);
                            break;
                        case PastWeek:
                            cal.add(Calendar.DATE, -7);
                            break;
                        case PastMonth:
                            cal.add(Calendar.MONTH, -1);
                            break;
                        case PastYear:
                            cal.add(Calendar.YEAR, -1);
                            break;
                        default:
                            break;
                    }
                    mRequest.limitLastUpdateTime(cal.getTime(), null);
                }

                if (mSearchFilters.mItemSize != BoxSearchFilters.ItemSize.Any) {
                    // Add filter for size
                    switch (mSearchFilters.mItemSize) {
                        case lessThanOneMb:
                            mRequest.limitSizeRange(0, ONE_MB);
                            break;
                        case OneMbToFiveMb:
                            mRequest.limitSizeRange(ONE_MB, 5*ONE_MB);
                            break;
                        case FiveMbToTwentyFiveMb:
                            mRequest.limitSizeRange(5*ONE_MB, 25*ONE_MB);
                            break;
                        case TwentyFiveMbToHundredMb:
                            mRequest.limitSizeRange(25*ONE_MB, 100*ONE_MB);
                            break;
                        case HundredMbToOneGB:
                            mRequest.limitSizeRange(100*ONE_MB, 1000*ONE_MB);
                            break;
                    }
                }
            }

            executeRequest();
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
            if (filteredItems.size() > 0 && !(filteredItems.get(0) instanceof ResultsHeader)) {
                mItems.add(0, new ResultsHeader(mParentFolder));
            }
            mAdapter.updateTo(mItems);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mRequest);
        outState.putInt(OUT_OFFSET, mOffset);
        if (mSearchQuery != null) {
            outState.putString(OUT_QUERY, mSearchQuery);
        }

        outState.putSerializable(EXTRA_SEARCH_FILTERS, mSearchFilters);
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

    @Override
    public void onCloseClicked(int position) {
        deleteFromRecentSearches(position);
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


        public Builder(BoxSession session, String searchQuery, BoxFolder parentFolder) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
            mArgs.putString(OUT_QUERY, searchQuery);
            mArgs.putSerializable(EXTRA_PARENT_FOLDER, BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()));
        }


            /**
             * @param session
             */
        public Builder(BoxSession session, BoxFolder parentFolder) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
            mArgs.putSerializable(EXTRA_PARENT_FOLDER, BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()));
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

    protected void deleteFromRecentSearches(int position) {
        mRecentSearches.remove(position);
        mRecentSearchesAdapter.notifyDataSetChanged();
        saveRecentSearches(mRecentSearches);
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
