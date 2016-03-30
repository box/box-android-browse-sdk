package com.box.androidsdk.browse.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.activities.BoxBrowseActivity;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.service.BoxBrowseController;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.browse.service.CompletionListener;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Base fragment for displaying box items. This class provides the internals needed to make requests
 * and update the ui on a response.
 */
public abstract class BoxBrowseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = BoxBrowseFragment.class.getName();

    protected static final int DEFAULT_LIMIT = 200;

    protected static final String ARG_ID = "argId";
    protected static final String ARG_USER_ID = "argUserId";
    protected static final String ARG_NAME = "argName";
    protected static final String ARG_LIMIT = "argLimit";
    protected static final String ARG_BOX_ITEM_FILTER = "argBoxBrowseFilter";

    protected static final String EXTRA_SECONDARY_ACTION_LISTENER = "com.box.androidsdk.browse.SECONDARYACTIONLISTENER";
    protected static final String EXTRA_MULTI_SELECT_HANDLER = "com.box.androidsdk.browse.MULTI_SELECT_HANDLER";
    protected static final String EXTRA_TITLE = "com.box.androidsdk.browse.TITLE";
    protected static final String EXTRA_COLLECTION = "com.box.androidsdk.browse.COLLECTION";
    protected static final String ACTION_FUTURE_TASK = "com.box.androidsdk.browse.FUTURE_TASK";

    protected BoxSession mSession;
    protected BoxIteratorItems mBoxIteratorItems;

    protected OnFragmentInteractionListener mListener;
    protected OnFragmentInteractionListener mSecondaryActionListener;
    protected MultiSelectHandler mMultiSelectHandler;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected ThumbnailManager mThumbnailManager;
    protected SwipeRefreshLayout mSwipeRefresh;
    protected ProgressBar mProgress;
    protected LocalBroadcastManager mLocalBroadcastManager;
    protected int mLimit = DEFAULT_LIMIT;

    private String mTitle;
    private boolean mWaitingForConnection;
    private boolean mIsConnected;
    private BrowseController mController;
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

    public BoxBrowseFragment() {
        // Required empty public constructor
    }

    public static void setAccentColor(Resources res, ProgressBar progressBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            int accentColor = res.getColor(R.color.box_accent);
            Drawable drawable = progressBar.getIndeterminateDrawable();
            if (drawable != null) {
                drawable.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                drawable.invalidateSelf();
            }
        }
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
            mThumbnailManager = initializeThumbnailManager();
            mLimit = getArguments().getInt(ARG_LIMIT);
            mBoxItemFilter = (BoxItemFilter) getArguments().getSerializable(ARG_BOX_ITEM_FILTER);
        }
        if (savedInstanceState != null) {
            setListItems((BoxIteratorItems) savedInstanceState.getSerializable(EXTRA_COLLECTION));
            if (savedInstanceState.containsKey(EXTRA_SECONDARY_ACTION_LISTENER)) {
                mSecondaryActionListener = (OnFragmentInteractionListener) savedInstanceState.getSerializable(EXTRA_SECONDARY_ACTION_LISTENER);
            }
            if (savedInstanceState.containsKey(EXTRA_MULTI_SELECT_HANDLER)) {
                mMultiSelectHandler = (MultiSelectHandler) savedInstanceState.getSerializable(EXTRA_MULTI_SELECT_HANDLER);
            }
        }

        // Initialize controller and listeners
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());

        // TODO: Do we really need this?
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, getIntentFilter());
        super.onResume();
    }

    @Override
    public void onPause() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        getActivity().unregisterReceiver(mConnectivityReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_COLLECTION, mBoxIteratorItems);
        outState.putSerializable(EXTRA_TITLE, mTitle);
        if (mSecondaryActionListener instanceof Serializable) {
            outState.putSerializable(EXTRA_SECONDARY_ACTION_LISTENER, (Serializable) mSecondaryActionListener);
        }
        if (mMultiSelectHandler instanceof Serializable) {
            outState.putSerializable(EXTRA_MULTI_SELECT_HANDLER, (Serializable) mMultiSelectHandler);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            setToolbar(savedInstanceState.getString(EXTRA_TITLE));
        }
    }

    private ThumbnailManager initializeThumbnailManager() {
        try {
            return new ThumbnailManager(getController().getThumbnailCacheDir());
        } catch (FileNotFoundException e) {
            BoxLogUtils.e(TAG, e);
            return null;
        }
    }

    protected void setToolbar(String name) {
        mTitle = name;
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ActionBar toolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(mTitle);
            }
        }
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
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.box_browsesdk_swipe_reresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeColors(R.color.box_accent);
        // This is a work around to show the loading circle because SwipeRefreshLayout.onMeasure must be called before setRefreshing to show the animation
        mSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        mItemsView = (RecyclerView) mRootView.findViewById(R.id.box_browsesdk_items_recycler_view);
        mItemsView.addItemDecoration(new BoxItemDividerDecoration(getResources()));
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mProgress = (ProgressBar) mRootView.findViewById(R.id.box_browsesdk_progress_bar);
        mAdapter = createBoxItemAdapter();
        mItemsView.setAdapter(mAdapter);
        if (getMultiSelectHandler() != null) {
            getMultiSelectHandler().setItemAdapter(mAdapter);
        }

        if (mBoxIteratorItems == null) {
            mProgress.setVisibility(View.VISIBLE);
            loadItems();
        } else {
            updateItems(mBoxIteratorItems);
        }
        return mRootView;
    }

    protected BoxItemAdapter createBoxItemAdapter(){
        return new BoxItemAdapter();
    }

    protected abstract void loadItems();

    protected void setListItems(final BoxIteratorItems items) {
        mBoxIteratorItems = items;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
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
                    .setCompletedListener(new CompletionListener(mLocalBroadcastManager));
        }
        return mController;
    }

    public void setController(BrowseController controller) {
        mController = controller;
    }

    protected OnFragmentInteractionListener getSecondaryActionListener() {
        return mSecondaryActionListener;
    }

    /**
     * Optionally set a secondary action to use on this fragment.
     *
     * @param listener listener to be called when a secondary action is clicked on an item. Must be serializable.
     * @param <T>      Serializable OnFragmentInteractionListener.
     */
    public <T extends OnFragmentInteractionListener & Serializable> void setSecondaryActionListener(T listener) {
        mSecondaryActionListener = listener;
        if (mAdapter != null) {
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
    }

    /**
     * @return the MultiSelectHandler set on this fragment. This is used in order to handle batch operations.
     */
    public MultiSelectHandler getMultiSelectHandler() {
        return mMultiSelectHandler;
    }

    /**
     * Optionally set a multi select handler.
     *
     * @param handler handler to be called when multiple items are selected.
     * @param <T>     Serializable OnFragmentInteractionListener.
     */
    public <T extends MultiSelectHandler & Serializable> void setMultiSelectHandler(T handler) {
        mMultiSelectHandler = handler;
        if (mAdapter != null) {
            mMultiSelectHandler.setItemAdapter(mAdapter);
        }
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
    protected void updateItems(final BoxIteratorItems items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        mProgress.setVisibility(View.GONE);
        mSwipeRefresh.setRefreshing(false);

        if (items == mBoxIteratorItems) {
            // if we are trying to display the original list no need to add.
            if (mAdapter.getItemCount() <= 0) {
                mAdapter.addAll(items);
            }
            return;
        }

        // Because we are always retrieving a folder with all items, we want to replace everything
        mAdapter.removeAll();
        mAdapter.addAll(items);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            }
        });
    }

    /**
     * Handles showing new thumbnails after they have been downloaded.
     *
     * @param intent
     */
    protected void onDownloadedThumbnail(final BoxResponseIntent intent) {
        if (mAdapter != null) {
            BoxListItem item = mAdapter.get(((BoxRequestsFile.DownloadThumbnail) intent.getRequest()).getId());
            if (item != null) {
                item.setResponse(intent);
                if (intent.isSuccess()) {
                    mAdapter.update(item.getIdentifier());
                }
            }
        }
    }

    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BoxRequestsFile.DownloadThumbnail.class.getName());
        return filter;
    }

    /**
     * Called when a {@link BoxListItem} is bound to a ViewHolder. Customizations of UI elements
     * should be done by overriding this method. If extending from a {@link BoxBrowseActivity}
     * a custom BoxBrowseFolder fragment can be returned in
     * {@link BoxBrowseActivity#createBrowseFolderFragment(BoxItem, BoxSession)}
     *
     * @param holder the BoxItemHolder
     */
    protected void onBindBoxItemViewHolder(BoxItemViewHolder holder) {
        if (holder.getItem() == null || holder.getItem().getBoxItem() == null) {
            return;
        }

        final BoxItem item = holder.getItem().getBoxItem();
        holder.getNameView().setText(item.getName());
        String description = "";
        if (item != null) {
            String modifiedAt = item.getModifiedAt() != null ?
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(item.getModifiedAt()).toUpperCase() :
                    "";
            String size = item.getSize() != null ?
                    localFileSizeToDisplay(item.getSize()) :
                    "";
            description = String.format(Locale.ENGLISH, "%s  • %s", modifiedAt, size);
            mThumbnailManager.setThumbnailIntoView(holder.getThumbView(), item);
        }
        holder.getMetaDescription().setText(description);
        holder.getProgressBar().setVisibility(View.GONE);
        holder.getMetaDescription().setVisibility(View.VISIBLE);
        holder.getThumbView().setVisibility(View.VISIBLE);
        if (!holder.getItem().getIsEnabled()) {
            holder.getView().setEnabled(false);
            holder.getNameView().setTextColor(getResources().getColor(R.color.box_browsesdk_hint));
            holder.getMetaDescription().setTextColor(getResources().getColor(R.color.box_browsesdk_disabled_hint));
            holder.getThumbView().setAlpha(0.26f);
        } else {
            holder.getView().setEnabled(true);
            holder.getNameView().setTextColor(getResources().getColor(R.color.box_browsesdk_primary_text));
            holder.getMetaDescription().setTextColor(getResources().getColor(R.color.box_browsesdk_hint));
            holder.getThumbView().setAlpha(1f);
        }
        if (getSecondaryActionListener() != null) {
            holder.getSecondaryAction().setVisibility(View.VISIBLE);
        } else {
            holder.getSecondaryAction().setVisibility(View.GONE);
        }

        if (getMultiSelectHandler() != null && getMultiSelectHandler().isEnabled()) {
            holder.getSecondaryAction().setVisibility(View.GONE);
            holder.getCheckBox().setVisibility(View.VISIBLE);
            holder.getCheckBox().setEnabled(getMultiSelectHandler().isSelectable(item));
            holder.getCheckBox().setChecked(getMultiSelectHandler().isItemSelected(item));
        } else {
            holder.getCheckBox().setVisibility(View.GONE);
        }

    }

    /**
     * Defines the conditions for when a BoxItem should be shown as enabled
     *
     * @param item the BoxItem that should be enabled or not
     * @return whether or not the BoxItem should be enabled
     */
    public boolean isItemEnabled(BoxItem item) {
        return true;
    }

    /**
     * Defines the conditions for when a BoxItem should be shown in the adapter
     *
     * @param item the BoxItem that should be visible or not
     * @return whether or not the BoxItem should be visible
     */
    public boolean isItemVisible(BoxItem item) {
        return true;
    }

    /**
     * Download the thumbnail for a given file.
     *
     * @param fileId file id to download thumbnail for.
     * @return A FutureTask that is tasked with fetching information on the given folder.
     */
    protected BoxRequestsFile.DownloadThumbnail getDownloadThumbnailTask(final String fileId, final File downloadLocation) {
        return getController().getThumbnailRequest(fileId, downloadLocation, getResources());
    }

    /**
     * Java version of routine to turn a long into a short user readable string.
     * <p/>
     * This routine is used if the JNI native C version is not available.
     *
     * @param numSize the number of bytes in the file.
     * @return String Short human readable String e.g. 2.5 MB
     */
    private String localFileSizeToDisplay(final double numSize) {
        final int constKB = 1024;
        final int constMB = constKB * constKB;
        final int constGB = constMB * constKB;
        final double floatKB = 1024.0f;
        final double floatMB = floatKB * floatKB;
        final double floatGB = floatMB * floatKB;
        final String BYTES = "B";
        String textSize = "0 bytes";
        String strSize = Double.toString(numSize);
        double size;

        if (numSize < constKB) {
            textSize = strSize + " " + BYTES;
        } else if ((numSize >= constKB) && (numSize < constMB)) {
            size = numSize / floatKB;
            textSize = String.format(Locale.ENGLISH, "%4.1f KB", size);
        } else if ((numSize >= constMB) && (numSize < constGB)) {
            size = numSize / floatMB;
            textSize = String.format(Locale.ENGLISH, "%4.1f MB", size);
        } else if (numSize >= constGB) {
            size = numSize / floatGB;
            textSize = String.format(Locale.ENGLISH, "%4.1f GB", size);
        }
        return textSize;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an item being tapped to be communicated to the activity
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called whenever an item in the RecyclerView is clicked and allows the activity to intercept
         * and override the behavior
         *
         * @param item the item that was clicked
         * @return whether the click event should continue to be handled by the fragment
         */
        boolean handleOnItemClick(BoxItem item);
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

        void toggle(BoxItem boxItem) {
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
        public void selectAll(){
            int totalItemCount = mItemAdapter.get().getItemCount();
            if (mSelectedItems.size() < totalItemCount) {
                int originalSize = mSelectedItems.size();
                for (BoxListItem item : mItemAdapter.get().mListItems){
                    BoxItem boxItem = item.getBoxItem();
                    if (boxItem != null && isSelectable(boxItem) && !isItemSelected(boxItem) ){
                        mSelectedItems.add(boxItem);
                        handleItemSelected(boxItem, true, this);
                    }
                }
                if (originalSize != mSelectedItems.size()){
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

    protected class BoxItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        BoxListItem mItem;

        View mView;
        ImageView mThumbView;
        TextView mNameView;
        TextView mMetaDescription;
        ProgressBar mProgressBar;
        ImageButton mSecondaryAction;
        BoxItemClickListener mSecondaryClickListener;
        AppCompatCheckBox mItemCheckBox;


        public BoxItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            if (getMultiSelectHandler() != null) {
                itemView.setOnLongClickListener(this);
            }
            mView = itemView;
            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mMetaDescription = (TextView) itemView.findViewById(R.id.metaline_description);
            mProgressBar = (ProgressBar) itemView.findViewById((R.id.spinner));
            mSecondaryAction = (ImageButton) itemView.findViewById(R.id.secondaryAction);
            mItemCheckBox = (AppCompatCheckBox) itemView.findViewById(R.id.boxItemCheckBox);
            mSecondaryClickListener = new BoxItemClickListener();
            mSecondaryAction.setOnClickListener(mSecondaryClickListener);
            setAccentColor(getResources(), mProgressBar);
        }

        public void bindItem(BoxListItem item) {
            mItem = item;
            mSecondaryClickListener.setListItem(item);
            onBindBoxItemViewHolder(this);
        }

        public void setError(BoxListItem item) {
            mItem = item;
            mItem.setState(BoxListItem.State.ERROR);
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            mThumbView.setImageResource(R.drawable.ic_box_browsesdk_refresh_grey_36dp);
            mThumbView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mMetaDescription.setVisibility(View.VISIBLE);
            mNameView.setText(activity.getResources().getString(R.string.box_browsesdk_error_retrieving_items));
            mMetaDescription.setText(activity.getResources().getString(R.string.box_browsesdk_tap_to_retry));
        }

        public void setLoading() {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            mThumbView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mMetaDescription.setVisibility(View.GONE);
            mNameView.setText(activity.getResources().getString(R.string.boxsdk_Please_wait));
        }

        public CheckBox getCheckBox() {
            return mItemCheckBox;
        }

        public ImageButton getSecondaryAction() {
            return mSecondaryAction;
        }


        public BoxListItem getItem() {
            return mItem;
        }

        public ProgressBar getProgressBar() {
            return mProgressBar;
        }

        public TextView getMetaDescription() {
            return mMetaDescription;
        }

        public TextView getNameView() {
            return mNameView;
        }

        public ImageView getThumbView() {
            return mThumbView;
        }

        public View getView() {
            return mView;
        }


        @Override
        public boolean onLongClick(View v) {
            if (getMultiSelectHandler() != null) {
                getMultiSelectHandler().setEnabled(!getMultiSelectHandler().isEnabled());
                getMultiSelectHandler().toggle(mItem.getBoxItem());
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if (getMultiSelectHandler() != null && getMultiSelectHandler().isEnabled()) {
                getMultiSelectHandler().toggle(mItem.getBoxItem());
                onBindBoxItemViewHolder(this);
                return;
            }
            if (mSwipeRefresh.isRefreshing()) {
                return;
            }
            if (mItem == null) {
                return;
            }

            if (mItem.getState() == BoxListItem.State.ERROR) {
                mItem.setState(BoxListItem.State.SUBMITTED);
                getController().execute(mItem.getRequest());
                setLoading();
            }

            if (mListener != null) {
                if (mListener.handleOnItemClick(mItem.getBoxItem())) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    if (mItem.getBoxItem() instanceof BoxFolder) {
                        // Find fragment container id
                        ViewGroup fragmentContainer = (ViewGroup) mRootView.getParent();
                        if (fragmentContainer != null) {
                            BoxFolder folder = (BoxFolder) mItem.getBoxItem();
                            FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();

                            // All fragments will always navigate into folders
                            BoxBrowseFolderFragment browseFolderFragment = new BoxBrowseFolderFragment
                                    .Builder((BoxFolder) folder, mSession).build();

                            trans.replace(fragmentContainer.getId(), browseFolderFragment)
                                    .addToBackStack(TAG)
                                    .commit();
                        }
                    }
                }
            }
        }
    }

    protected class BoxItemAdapter extends RecyclerView.Adapter<BoxItemViewHolder> {
        protected ArrayList<BoxListItem> mListItems = new ArrayList<BoxListItem>();
        protected HashMap<String, BoxListItem> mItemsMap = new HashMap<String, BoxListItem>();


        @Override
        public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
            return new BoxItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BoxItemViewHolder boxItemHolder, int i) {
            BoxListItem item = mListItems.get(i);
            if (item.getState() == BoxListItem.State.ERROR) {
                boxItemHolder.setError(item);
                return;
            }
            boxItemHolder.bindItem(item);

            // Fetch thumbnails for media file types
            if (item.getBoxItem() instanceof BoxFile && ThumbnailManager.isThumbnailAvailable(item.getBoxItem())) {
                if (item.getRequest() == null) {
                    item.setRequest(getDownloadThumbnailTask(item.getBoxItem().getId(), mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId())));
                } else if (item.getResponse() != null) {
                     BoxException ex = (BoxException) item.getResponse().getException();
                    if (ex != null && ex.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                        item.setState(BoxListItem.State.CREATED);
                    }
                }
            }

            // Execute a request if it hasn't been done so already
            if (item.getRequest() != null && item.getState() == BoxListItem.State.CREATED) {
                item.setState(BoxListItem.State.SUBMITTED);
                getController().execute(item.getRequest());
                if (item.getIdentifier().equals(ACTION_FUTURE_TASK)) {
                    boxItemHolder.setLoading();
                }
                return;
            }
        }

        @Override
        public int getItemCount() {
            return mListItems.size();
        }

        public BoxListItem get(String id) {
            return mItemsMap.get(id);
        }

        public synchronized void removeAll() {
            mItemsMap.clear();
            mListItems.clear();
        }

        public void remove(BoxListItem listItem) {
            remove(listItem.getIdentifier());
        }

        public synchronized void remove(String key) {
            BoxListItem item = mItemsMap.remove(key);
            if (item != null) {
                int index = item.getPosition() != null ?
                        item.getPosition().intValue() :
                        mListItems.indexOf(item);
                mListItems.remove(index);
                this.notifyItemRemoved(index);
            }
        }

        public void addAll(BoxIteratorItems items) {
            for (BoxItem item : items) {
                if (!mItemsMap.containsKey(item.getId())) {
                    add(new BoxListItem(item, item.getId()));
                } else {
                    // update an existing item if it exists.
                    mItemsMap.get(item.getId()).setBoxItem(item);
                }
            }
        }

        public synchronized void add(BoxListItem listItem) {
            if (listItem.getBoxItem() != null) {
                // If the item should not be visible, skip adding the item
                if (!isItemVisible(listItem.getBoxItem())) {
                    return;
                }
                //Filter out item if necessary
                if (mBoxItemFilter != null && !mBoxItemFilter.accept(listItem.getBoxItem())) {
                    return;
                }
                listItem.setIsEnabled(isItemEnabled(listItem.getBoxItem()));
            }
            mListItems.add(listItem);
            listItem.setPosition(mListItems.size() - 1);
            mItemsMap.put(listItem.getIdentifier(), listItem);
        }

        public void update(String id) {
            BoxListItem item = mItemsMap.get(id);
            if (item != null) {
                int index = item.getPosition() != null ?
                        item.getPosition() :
                        mListItems.indexOf(item);
                notifyItemChanged(index);
            }
        }

        public void update(BoxItem item) {
            BoxListItem listItem = mItemsMap.get(item.getId());
            if (listItem != null) {
                listItem.setBoxItem(item);
                int index = listItem.getPosition() != null ?
                        listItem.getPosition() :
                        mListItems.indexOf(listItem);
                notifyItemChanged(index);
            }
        }
    }

    private class BoxItemClickListener implements View.OnClickListener {

        protected BoxListItem mBoxListItem;

        void setListItem(BoxListItem listItem) {
            mBoxListItem = listItem;
        }

        @Override
        public void onClick(View v) {
            getSecondaryActionListener().handleOnItemClick(mBoxListItem.getBoxItem());
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
         * Set the number of items that the results will be limited to when retrieving folder items
         *
         * @param limit
         */
        public void setLimit(int limit) {
            mArgs.putInt(ARG_LIMIT, limit);
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
