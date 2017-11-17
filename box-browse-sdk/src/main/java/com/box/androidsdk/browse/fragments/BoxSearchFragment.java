package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.activities.FilterSearchResults;
import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.adapters.BoxSearchAdapter;
import com.box.androidsdk.browse.adapters.ResultsHeader;
import com.box.androidsdk.browse.models.BoxSearchFilters;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {

    private static String EXTRA_PARENT_FOLDER = "SearchFragment.ExtraParentFolder";

    // parent folder for the search
    private static final String OUT_ITEM = "outItem";

    // Offset for the next search request
    private static final String OUT_OFFSET = "outOffset";

    // Current search term
    private static final String OUT_QUERY = "outQuery";

    private static final int DEFAULT_LIMIT = 20;
    private int mLimit;
    protected int mOffset = 0;


    protected String mSearchQuery;
    protected BoxRequestsSearch.Search mRequest;

    public static final int REQUEST_FILTER_SEARCH_RESULTS = 228;
    public static final String EXTRA_SEARCH_FILTERS = "SearchFragment.SearchFilters";

    private View mSearchFiltersHeader;
    protected BoxSearchFilters mSearchFilters;
    private HashMap<BoxSearchFilters.ItemType, String[]> mItemTypeToExtensions;

    // Size of 1MB as per search API
    private final static long ONE_MB = 1000000;

    private BoxFolder mParentFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchQuery = null;
        if (getArguments() != null) {
            mLimit = getArguments().getInt(ARG_LIMIT, DEFAULT_LIMIT);
            mSearchQuery = getArguments().getString(OUT_QUERY, null);
            mParentFolder = (BoxFolder) getArguments().getSerializable(EXTRA_PARENT_FOLDER);
            mSearchFilters = (BoxSearchFilters) getArguments().getSerializable(EXTRA_SEARCH_FILTERS);
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
    }

    @Override
    protected int getLayout() {
        return R.layout.box_browsesdk_fragment_search;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mSearchFiltersHeader = view.findViewById(R.id.filterResultsHeader);
        mSearchFiltersHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFilterActivity();
            }
        });
        setupSearchFiltersHeader();
        // Select folder button shall be disabled during search
        updateSelectFolderButtonState(false);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore select folder button
        updateSelectFolderButtonState(true);
    }

    private void updateSelectFolderButtonState(boolean enabled) {
        View selectFolderButton = getActivity().findViewById(R.id.box_browsesdk_select_folder_button);
        if(selectFolderButton != null) {
            selectFolderButton.setEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        search(mSearchQuery);
    }

    /**
     * Start filter activity.
     */
    protected void startFilterActivity() {
        startActivityForResult(FilterSearchResults.newFilterSearchResultsIntent(getActivity(), mSearchFilters), REQUEST_FILTER_SEARCH_RESULTS);
    }

    protected void setupSearchFiltersHeader() {
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
                    filters.add(type.getString(getContext()));
                }
            }

            if (mSearchFilters.mItemModifiedDate != BoxSearchFilters.ItemModifiedDate.Any) {
                filters.add(mSearchFilters.mItemModifiedDate.getString(getContext()));
            }

            if (mSearchFilters.mItemSize != BoxSearchFilters.ItemSize.Any) {
                filters.add(mSearchFilters.mItemSize.getString(getContext()));
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

    /**
     * Returns the parent folder in which the search should be performed
     * @return parent folder in which the search should be performed
     */
    public BoxFolder getParentFolder() {
        return mParentFolder;
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsSearch.Search.class.getName());
        return filter;
    }

    /**
     * Gets search query.
     *
     * @return the search query
     */
    public String getSearchQuery() {
        if (mSearchQuery != null) {
            return mSearchQuery;
        }

        return "";
    }

    /**
     * Search.
     *
     * @param query the query
     */
    public void search(String query) {
        if (query != null) {
            String trimmedQuery = query.trim();
            if (!trimmedQuery.equals(mSearchQuery) || mRequest == null) {
                mSearchQuery = trimmedQuery;
                search();
            }
        }
    }

    /**
     * Search.
     */
    protected void search() {
        if (mSearchQuery != null && !mSearchQuery.equals("")) {
            mRequest = getController().getSearchRequest(mSearchQuery);
            mAdapter.removeAll();
            loadItems();
            mItems = null;
            mAdapter.notifyDataSetChanged();
            notifyUpdateListeners();
        } else {
            mRequest = null;
            mProgress.setVisibility(View.GONE);
            mSearchFiltersHeader.setVisibility(View.GONE);
            mItems = null;
            mAdapter.removeAll();
            mAdapter.notifyDataSetChanged();
            notifyUpdateListeners();
        }
    }

    /**
     * Execute request.
     */
    protected void executeRequest() {
        getController().execute(mRequest);
    }

    @Override
    protected void loadItems() {
        if (mRequest != null) {
            mProgress.setVisibility(View.VISIBLE);
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
                                extensions.add(extension);
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
    protected BoxItemAdapter createAdapter() {
        return new BoxSearchAdapter(getActivity(), getController(), this);
    }

    /**
     * Update to.
     *
     * @param items the items
     */
    protected void updateTo(ArrayList<BoxItem> items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        mProgress.setVisibility(View.GONE);
        mSearchFiltersHeader.setVisibility(View.VISIBLE);

        mItems = new ArrayList<BoxItem>();
        mItems.addAll(items);
        if (mItems.size() > 0 && !(mItems.get(0) instanceof ResultsHeader)) {
            mItems.add(0, new ResultsHeader(mParentFolder));
        }
        mAdapter.updateTo(mItems);
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

    /**
     * On items fetched.
     *
     * @param response the response received from Box server
     */
    protected void onItemsFetched(BoxResponse response) {
        if (!response.isSuccess()) {
            checkConnectivity();
            return;
        }

        if (response.getRequest().equals(mRequest)) {
            ArrayList<String> removeIds = new ArrayList<String>(1);
            removeIds.add(BoxSearchAdapter.LOAD_MORE_ID);
            mAdapter.remove(removeIds);

            if (response.getResult() instanceof BoxIteratorItems) {
                BoxIteratorItems items = (BoxIteratorItems) response.getResult();

                if (((BoxRequestsSearch.Search) response.getRequest()).getOffset() == 0) {
                    mOffset = 0;
                    updateTo(items.getEntries());
                } else {
                    updateItems(items.getEntries());
                }
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
            mSearchFiltersHeader.setVisibility(View.VISIBLE);
        }
    }

    /**
     * If for some reason the server returns less than the right number of items
     * for instance if some results are hidden due to permissions offset based off of number of items
     * will not be a multiple of limit.
     * @param itemsSize
     * @param limit
     * @return offset that should be used with next request
     */
    private static int calculateBestOffset(int itemsSize, int limit){
        double offset = ((double) itemsSize)/limit;
        return (int)Math.ceil(offset) * limit;
    }

    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static class Builder extends BoxBrowseFragment.Builder<BoxSearchFragment> {


        /**
         * Instantiates a new Builder.
         *
         * @param session      the session
         * @param searchQuery  the search query
         * @param parentFolder the parent folder in which the search should be performed
         */
        public Builder(BoxSession session, String searchQuery, BoxFolder parentFolder) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
            mArgs.putString(OUT_QUERY, searchQuery);
            mArgs.putSerializable(EXTRA_PARENT_FOLDER, BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()));
        }

        /**
         * Instantiates a new Builder.
         *
         * @param session      the session
         * @param searchQuery  the search query
         * @param parentFolder the parent folder in which the search should be performed
         * @param boxSearchFilters Filters to fine tune the search results based on file types, modification times etc.
         */
        public Builder(BoxSession session, String searchQuery, BoxFolder parentFolder, BoxSearchFilters boxSearchFilters) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
            mArgs.putString(OUT_QUERY, searchQuery);
            mArgs.putSerializable(EXTRA_PARENT_FOLDER, BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()));
            mArgs.putSerializable(EXTRA_SEARCH_FILTERS, boxSearchFilters);
        }

        /**
         * Instantiates a new Builder.
         *
         * @param session      the session
         * @param parentFolder the parent folder
         */
        public Builder(BoxSession session, BoxFolder parentFolder) {
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
            mArgs.putSerializable(EXTRA_PARENT_FOLDER, BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()));
        }

        /**
         * Set the number of items that the results will be limited to when retrieving search results
         *
         * @param limit the limit
         * @return the limit
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
