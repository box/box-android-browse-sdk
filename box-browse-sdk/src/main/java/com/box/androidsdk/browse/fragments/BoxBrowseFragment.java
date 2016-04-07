package com.box.androidsdk.browse.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.service.BoxBrowseController;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.browse.service.CompletionListener;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base fragment for displaying box items. This class provides the internals needed to make requests
 * and update the ui on a response.
 */
public abstract class BoxBrowseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        BoxItemAdapter.OnInteractionListener {
    public static final String TAG = BoxBrowseFragment.class.getName();

    protected static final String ARG_ID = "argId";
    protected static final String ARG_USER_ID = "argUserId";
    protected static final String ARG_NAME = "argName";
    protected static final String ARG_LIMIT = "argLimit";
    protected static final String ARG_BOX_ITEM_FILTER = "argBoxBrowseFilter";

    protected static final String EXTRA_SECONDARY_ACTION_LISTENER = "com.box.androidsdk.browse.SECONDARYACTIONLISTENER";
    protected static final String EXTRA_MULTI_SELECT_HANDLER = "com.box.androidsdk.browse.MULTI_SELECT_HANDLER";
    protected static final String EXTRA_TITLE = "com.box.androidsdk.browse.TITLE";
    protected static final String EXTRA_COLLECTION = "com.box.androidsdk.browse.COLLECTION";

    protected ArrayList<BoxItem> mItems;
    protected BoxSession mSession;

    protected OnItemClickListener mListener;
    protected OnSecondaryActionListener mSecondaryActionListener;
    protected MultiSelectHandler mMultiSelectHandler;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected SwipeRefreshLayout mSwipeRefresh;
    protected ProgressBar mProgress;

    private String mTitle;
    private boolean mWaitingForConnection;
    private boolean mIsConnected;
    private BrowseController mController;
    private Set<OnUpdateListener> mUpdateListeners = new HashSet<OnUpdateListener>();
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent instanceof BoxResponseIntent) {
                handleResponse((BoxResponseIntent) intent);
            }
        }
    };
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                mIsConnected = (networkInfo != null && networkInfo.isConnected());
                if (mWaitingForConnection && mIsConnected) {
                    mWaitingForConnection = false;
                    onRefresh();
                }
            }
        }
    };
    private View mRootView;
    private BoxItemFilter mBoxItemFilter;
    private LocalBroadcastManager mLocalBroadcastmanager;
    private ImageView mEmptyFolder;

    public BoxBrowseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String userId = getArguments().getString(ARG_USER_ID);
            if (SdkUtils.isBlank(userId)) {
                throw new IllegalArgumentException("A valid session or user id must be provided");
            }
            mSession = new BoxSession(getActivity(), userId);
            mBoxItemFilter = (BoxItemFilter) getArguments().getSerializable(ARG_BOX_ITEM_FILTER);
        }
        if (savedInstanceState != null) {
            mItems = (ArrayList<BoxItem>) savedInstanceState.getSerializable(EXTRA_COLLECTION);
            if (savedInstanceState.containsKey(EXTRA_SECONDARY_ACTION_LISTENER)) {
                mSecondaryActionListener = (OnSecondaryActionListener) savedInstanceState.getSerializable(EXTRA_SECONDARY_ACTION_LISTENER);
            }
            if (savedInstanceState.containsKey(EXTRA_MULTI_SELECT_HANDLER)) {
                mMultiSelectHandler = (MultiSelectHandler) savedInstanceState.getSerializable(EXTRA_MULTI_SELECT_HANDLER);
            }
        }

        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        getLocalBroadcastManager().registerReceiver(mBroadcastReceiver, getIntentFilter());
        super.onResume();
    }

    @Override
    public void onPause() {
        getLocalBroadcastManager().unregisterReceiver(mBroadcastReceiver);
        getActivity().unregisterReceiver(mConnectivityReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_COLLECTION, mItems);
        outState.putSerializable(EXTRA_TITLE, mTitle);
        if (mSecondaryActionListener instanceof Serializable) {
            outState.putSerializable(EXTRA_SECONDARY_ACTION_LISTENER, (Serializable) mSecondaryActionListener);
        }
        if (mMultiSelectHandler instanceof Serializable) {
            outState.putSerializable(EXTRA_MULTI_SELECT_HANDLER, (Serializable) mMultiSelectHandler);
        }
        super.onSaveInstanceState(outState);
    }

    protected void handleResponse(BoxResponseIntent intent) {
        if (!intent.isSuccess()) {
            mController.onError(getActivity(), intent.getResponse());
        }
        if (intent.getAction().equals(BoxRequestsFile.DownloadThumbnail.class.getName())) {
            onDownloadedThumbnail(intent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.box_browsesdk_fragment_browse, container, false);
        mEmptyFolder = (ImageView) mRootView.findViewById(R.id.box_browsesdk_folder_empty);
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.box_browsesdk_swipe_reresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeColors(R.color.box_accent);
        // This is a work around to show the loading circle because SwipeRefreshLayout.onMeasure must be called before setRefreshing to show the animation
        mSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        mItemsView = (RecyclerView) mRootView.findViewById(R.id.box_browsesdk_items_recycler_view);
        mItemsView.addItemDecoration(new BoxItemDividerDecoration(getResources()));
        mItemsView.addItemDecoration(new FooterDecoration(getResources()));
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mProgress = (ProgressBar) mRootView.findViewById(R.id.box_browsesdk_progress_bar);
        mAdapter = createAdapter();

        mAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                updateUI();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateUI();
            }
        });

        mItemsView.setAdapter(mAdapter);
        if (getMultiSelectHandler() != null) {
            getMultiSelectHandler().setItemAdapter(mAdapter);
        }


        if (mItems == null) {
            mProgress.setVisibility(View.VISIBLE);
            loadItems();
        } else {
            updateItems(mItems);
        }
        return mRootView;
    }

    protected abstract void loadItems();

    private void updateUI() {
        if (mItems == null) {
            // UI should not be updated before the first load
            return;
        }
        final int emptyFolderVisibility = mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE;
        mEmptyFolder.setVisibility(emptyFolderVisibility);
    }

    protected BoxItemAdapter createAdapter() {
        return new BoxItemAdapter(getActivity(), getController(), this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnItemClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnItemClickListener");
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        loadItems();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public BrowseController getController() {
        if (mController == null) {
            mController = new BoxBrowseController(mSession, new BoxApiFile(mSession), new BoxApiFolder(mSession), new BoxApiSearch(mSession))
                    .setCompletedListener(new CompletionListener(getLocalBroadcastManager()));
        }
        return mController;
    }

    public void setController(BrowseController controller) {
        mController = controller;
    }

    public OnSecondaryActionListener getOnSecondaryActionListener() {
        return mSecondaryActionListener;
    }

    @Override
    public OnItemClickListener getOnItemClickListener() {
        return (OnItemClickListener) getActivity();
    }

    /**
     * Optionally set a secondary action to use on this fragment.
     *
     * @param listener listener to be called when a secondary action is clicked on an item. Must be serializable.
     * @param <T>      Serializable OnSecondaryActionListener.
     */
    public <T extends OnSecondaryActionListener & Serializable> void setSecondaryActionListener(T listener) {
        mSecondaryActionListener = listener;
        if (mAdapter != null) {
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
    }

    /**
     * @return the MultiSelectHandler set on this fragment. This is used in order to handle batch operations.
     */
    @Override
    public MultiSelectHandler getMultiSelectHandler() {
        return mMultiSelectHandler;
    }

    /**
     * Optionally set a multi select handler.
     *
     * @param handler handler to be called when multiple items are selected.
     * @param <T>     Serializable OnSecondaryActionListener.
     */
    public <T extends MultiSelectHandler & Serializable> void setMultiSelectHandler(T handler) {
        mMultiSelectHandler = handler;
        if (mAdapter != null) {
            mMultiSelectHandler.setItemAdapter(mAdapter);
        }
    }

    protected LocalBroadcastManager getLocalBroadcastManager() {
        if (mLocalBroadcastmanager == null) {
            mLocalBroadcastmanager = LocalBroadcastManager.getInstance(getActivity());
        }
        return mLocalBroadcastmanager;
    }

    /**
     * @return BoxItemFilter set on this fragment: passed during creation in the intent
     */
    public BoxItemFilter getItemFilter() {
        return mBoxItemFilter;
    }

    /**
     * Call on loading error and refresh if loss of connectivity is the suspect.
     */
    protected void checkConnectivity() {
        mWaitingForConnection = !mIsConnected;
    }

    /**
     * Updates the list of items that the adapter is bound to
     */
    protected void updateItems(final ArrayList<BoxItem> items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mProgress.setVisibility(View.GONE);
        mSwipeRefresh.setRefreshing(false);

        ArrayList<BoxItem> filteredItems = new ArrayList<BoxItem>();
        for (BoxItem item : items) {
            if (getItemFilter() != null && !getItemFilter().accept(item)) {
                continue;
            }
            filteredItems.add(item);
        }
        mItems = filteredItems;
        mAdapter.updateTo(mItems);
    }

    /**
     * Handles showing new thumbnails after they have been downloaded.
     *
     * @param intent
     */
    protected void onDownloadedThumbnail(final BoxResponseIntent intent) {
        if (mAdapter != null) {
            int index = mAdapter.indexOf(((BoxRequestsFile.DownloadThumbnail)intent.getRequest()).getId());
            mAdapter.notifyItemChanged(index);
        }
    }

    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BoxRequestsFile.DownloadThumbnail.class.getName());
        return filter;
    }

    public interface OnSecondaryActionListener {
        boolean onSecondaryAction(BoxItem item);
    }

    public interface OnItemClickListener {
        void onItemClick(BoxItem item);
    }

    public static abstract class MultiSelectHandler {


        HashSet<BoxItem> mSelectedItems = new HashSet<BoxItem>();
        boolean mIsMultiSelecting;
        transient WeakReference<BoxItemAdapter> mItemAdapter;

        /**
         *
         * @return a list of selected items.
         */
        public List<BoxItem> getSelectedBoxItems() {
            ArrayList<BoxItem> items = new ArrayList<BoxItem>(mSelectedItems.size());
            items.addAll(mSelectedItems);
            return items;
        }

        /**
         *
         * @return the number of items selected.
         */
        public int getSize() {
            return mSelectedItems.size();
        }

        /**
         *
         * @param item a box item being displayed in this fragment.
         * @return true if the item is selected.
         */
        public boolean isItemSelected(BoxItem item) {
            return mSelectedItems.contains(item);
        }

        /**
         * @param boxItem box item the user may potentially select.
         * @return true if this item can be selected by the user, false if it should be disabled.
         */
        public abstract boolean isSelectable(BoxItem boxItem);

        /**
         * Called when a user selects or deselects an item in the list.
         *
         * @param boxItem     box item the user clicked while in multi select mode.
         * @param wasSelected whether or not the the item the user clicked was selected.
         * @param handler     the handler that keeps track of which files were chosen.
         */
        public abstract void handleItemSelected(BoxItem boxItem, boolean wasSelected, MultiSelectHandler handler);

        public void toggle(BoxItem boxItem) {
            if (boxItem == null || !isSelectable(boxItem)) {
                return;
            }
            boolean wasSelected = false;
            if (isItemSelected(boxItem)) {
                mSelectedItems.remove(boxItem);
            } else {
                mSelectedItems.add(boxItem);
                wasSelected = true;
            }
            handleItemSelected(boxItem, wasSelected, this);
        }

        void setItemAdapter(BoxItemAdapter adapter) {
            mItemAdapter = new WeakReference<BoxItemAdapter>(adapter);
        }

        /**
         * Select all known box items and update ui.
         */
        public void selectAll() {
            int totalItemCount = mItemAdapter.get().getItemCount();
            if (mSelectedItems.size() < totalItemCount) {
                int originalSize = mSelectedItems.size();
                for (BoxItem boxItem : mItemAdapter.get().getItems()) {
                    if (boxItem != null && isSelectable(boxItem) && !isItemSelected(boxItem)) {
                        mSelectedItems.add(boxItem);
                        handleItemSelected(boxItem, true, this);
                    }
                }
                if (originalSize != mSelectedItems.size()) {
                    mItemAdapter.get().notifyItemRangeChanged(0, mItemAdapter.get().getItemCount());
                }
            }
        }

        /**
         * Deselect all selected box items and update ui.
         */
        public void deselectAll(){
            if (mSelectedItems.size() > 0) {
                mSelectedItems.clear();
                mItemAdapter.get().notifyItemRangeChanged(0, mItemAdapter.get().getItemCount());
            }
         }

         /**
         * Return true if multi selecting, false otherwise.
         */
        public boolean isEnabled() {
            return mIsMultiSelecting;
        }

        /**
         * Enable or disable multiselect mode.
         */
        public void setEnabled(boolean enabled) {
            if (mIsMultiSelecting == enabled) {
                return;
            }
            mIsMultiSelecting = enabled;
            if (enabled == false) {
                mSelectedItems.clear();
            }
            if (mItemAdapter != null && mItemAdapter.get() != null) {
                mItemAdapter.get().notifyItemRangeChanged(0, mItemAdapter.get().getItemCount());
            }
        }

    }

    public void addOnUpdateListener(OnUpdateListener updateListener) {
        synchronized (mUpdateListeners) {
            mUpdateListeners.add(updateListener);
        }

    }

    public void removeOnUpdateListener(OnUpdateListener updateListener) {
        synchronized (mUpdateListeners) {
            mUpdateListeners.remove(updateListener);
        }

    }

    @Override
    public void onDestroy() {
        synchronized (mUpdateListeners) {
            mUpdateListeners.clear();
        }

        super.onDestroy();
    }

    protected void notifyUpdateListeners() {
        synchronized (mUpdateListeners) {
            for (OnUpdateListener listener : mUpdateListeners) {
                listener.onUpdate();
            }
        }
    }

    private class BoxItemDividerDecoration extends RecyclerView.ItemDecoration {
        Drawable mDivider;

        public BoxItemDividerDecoration(Resources resources) {
            mDivider = resources.getDrawable(R.drawable.box_browsesdk_item_divider);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    private static class FooterDecoration extends RecyclerView.ItemDecoration {
        private final int mFooterPadding;

        public FooterDecoration(Resources resources) {
            mFooterPadding = (int) resources.getDimension(R.dimen.box_browsesdk_list_footer_padding);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() -1) {
                outRect.bottom = mFooterPadding;
            }
        }
    }

    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static abstract class Builder<T extends BoxBrowseFragment> {
        protected Bundle mArgs = new Bundle();


        protected void setFolderId(String folderId) {
            mArgs.putString(ARG_ID, folderId);
        }

        protected void setFolderName(String folderName) {
            mArgs.putString(ARG_NAME, folderName);
        }

        protected void setUserId(String userId) {
            mArgs.putString(ARG_USER_ID, userId);
        }

        /**
         * Set the BoxItemFilter for filtering the items being displayed
         *
         * @param filter
         * @param <E>
         */
        public <E extends Serializable & BoxItemFilter> void setBoxItemFilter(E filter) {
            mArgs.putSerializable(ARG_BOX_ITEM_FILTER, filter);
        }

        /**
         * Returns an empty instance of the fragment to build
         *
         * @return
         */
        protected abstract T getInstance();

        public T build() {
            T newFragment = getInstance();
            newFragment.setArguments(mArgs);
            return newFragment;
        }
    }
}
