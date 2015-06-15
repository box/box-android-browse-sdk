package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.activities.BoxBrowseActivity;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BoxBrowseFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public abstract class BoxBrowseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    public static final String ARG_ID = "argId";
    public static final String ARG_USER_ID = "argUserId";
    public static final String ARG_NAME = "argName";

    public static final String TAG = BoxBrowseFragment.class.getName();
    protected static final int DEFAULT_LIMIT = 1000;

    protected static final String ACTION_FETCHED_ITEMS = "BoxBrowseFragment_FetchedItems";
    protected static final String ACTION_FETCHED_INFO = "BoxBrowseFragment_FetchedInfo";
    protected static final String ACTION_DOWNLOADED_FILE_THUMBNAIL = "BoxBrowseFragment_DownloadedFileThumbnail";
    protected static final String EXTRA_SUCCESS = "BoxBrowseFragment_ArgSuccess";
    protected static final String EXTRA_EXCEPTION = "BoxBrowseFragment_ArgException";
    protected static final String EXTRA_ID = "BoxBrowseFragment_FolderId";
    protected static final String EXTRA_FILE_ID = "BoxBrowseFragment_FileId";
    protected static final String EXTRA_OFFSET = "BoxBrowseFragment_ArgOffset";
    protected static final String EXTRA_LIMIT = "BoxBrowseFragment_Limit";
    protected static final String EXTRA_FOLDER = "BoxBrowseFragment_Folder";
    protected static final String EXTRA_COLLECTION = "BoxBrowseFragment_Collection";

    protected String mUserId;
    protected BoxSession mSession;

    private BoxListItems mBoxListItems;

    protected OnFragmentInteractionListener mListener;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected ThumbnailManager mThumbnailManager;
    protected SwipeRefreshLayout mSwipeRefresh;

    protected LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DOWNLOADED_FILE_THUMBNAIL)) {
                onDownloadedThumbnail(intent);
            } else {
                // Handle response
                if (intent.getAction().equals(ACTION_FETCHED_INFO)) {
                    onInfoFetched(intent);
                } else if (intent.getAction().equals(ACTION_FETCHED_ITEMS)) {
                    onItemsFetched(intent);
                }

                // Remove refreshing icon
                if (mSwipeRefresh != null) {
                    mSwipeRefresh.setRefreshing(false);
                }

            }
        }
    };

    private static ThreadPoolExecutor mApiExecutor;
    private static ThreadPoolExecutor mThumbnailExecutor;

    protected ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }

    /**
     * Executor that we will submit thumbnail tasks to.
     *
     * @return executor
     */
    protected ThreadPoolExecutor getThumbnailExecutor() {
        if (mThumbnailExecutor == null || mThumbnailExecutor.isShutdown()) {
            mThumbnailExecutor = new ThreadPoolExecutor(1, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mThumbnailExecutor;
    }

    protected IntentFilter initializeIntentFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FETCHED_INFO);
        filter.addAction(ACTION_FETCHED_ITEMS);
        filter.addAction(ACTION_DOWNLOADED_FILE_THUMBNAIL);
        return filter;
    }

    public BoxBrowseFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize broadcast managers
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        if (getArguments() != null) {
            mUserId = getArguments().getString(ARG_USER_ID);
            mThumbnailManager = initializeThumbnailManager();
            mUserId = getArguments().getString(ARG_USER_ID);
            mSession = new BoxSession(getActivity(), mUserId);
        }
        if (savedInstanceState != null) {
            setListItem((BoxListItems) savedInstanceState.getSerializable(EXTRA_COLLECTION));
        }
    }

    @Override
    public void onResume() {
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, initializeIntentFilters());
        super.onResume();
    }

    @Override
    public void onPause() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_COLLECTION, mBoxListItems);
        super.onSaveInstanceState(outState);
    }

    private ThumbnailManager initializeThumbnailManager() {
        try {
            return new ThumbnailManager(getActivity().getCacheDir());
        } catch (FileNotFoundException e) {
            // TODO: Call error handler
            return null;
        }
    }

    protected void setToolbar(String name) {
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ActionBar toolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(name);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.box_browsesdk_fragment_browse, container, false);
        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.box_browsesdk_swipe_reresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeColors(R.color.box_accent);
        // This is a work around to show the loading circle because SwipeRefreshLayout.onMeasure must be called before setRefreshing to show the animation
        mSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        mItemsView = (RecyclerView) rootView.findViewById(R.id.box_browsesdk_items_recycler_view);
        mItemsView.addItemDecoration(new BoxItemDividerDecoration(getResources()));
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new BoxItemAdapter();
        mItemsView.setAdapter(mAdapter);

        if (mBoxListItems == null) {
            mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        } else {
            displayBoxList(mBoxListItems);

        }
        return rootView;
    }

    protected void setListItem(final BoxListItems items) {
        mBoxListItems = items;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        getApiExecutor().execute(fetchInfo());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    protected void onInfoFetched(Intent intent) {
        onItemsFetched(intent);
    }

    /**
     * Fetch the first information relevant to this fragment or what should be used for refreshing.
     *
     * @return A FutureTask that is tasked with fetching the first information relevant to this fragment or what should be used for refreshing.
     */
    public abstract FutureTask<Intent> fetchInfo();

    /**
     * Handle showing a collection in the given intent.
     *
     * @param intent an intent that contains a collection in EXTRA_COLLECTION.
     */
    protected void onItemsFetched(Intent intent) {
        if (intent.getBooleanExtra(EXTRA_SUCCESS, true)) {
            mAdapter.remove(intent.getAction());
        } else {
            BoxListItem item = mAdapter.get(intent.getAction());
            if (item != null) {
                item.setIsError(true);
                if (intent.getAction().equals(ACTION_FETCHED_INFO)) {
                    item.setTask(fetchInfo());
                } else if (intent.getAction().equals(ACTION_FETCHED_ITEMS)) {
                    int limit = intent.getIntExtra(EXTRA_LIMIT, DEFAULT_LIMIT);
                    int offset = intent.getIntExtra(EXTRA_OFFSET, 0);
                    item.setTask(fetchItems(offset, limit));
                }
                mAdapter.update(intent.getAction());
            }
            return;
        }
        displayBoxList((BoxListItems) intent.getSerializableExtra(EXTRA_COLLECTION));
        mSwipeRefresh.setRefreshing(false);
    }

    /**
     * show in this fragment a box list of items.
     */
    protected void displayBoxList(final BoxListItems items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        // if we are trying to display the original list no need to add.
        if (items == mBoxListItems) {

            //  mBoxListItems.addAll(items);
            if (mAdapter.getItemCount() < 1) {
                mAdapter.addAll(items);
            }
        } else {
            if (mBoxListItems == null) {
                setListItem(items);
            }
            mBoxListItems.addAll(items);
            mAdapter.addAll(items);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

        int limit = DEFAULT_LIMIT;
        if (items.limit() != null && items.limit() > 0) {
            limit = items.limit().intValue();
        }
        if (mBoxListItems.size() < items.fullSize()) {
            // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
            mAdapter.add(new BoxListItem(fetchItems(mBoxListItems.size(), limit),
                    ACTION_FETCHED_ITEMS));
        }

    }

    protected abstract FutureTask<Intent> fetchItems(final int offset, final int limit);

    /**
     * Handles showing new thumbnails after they have been downloaded.
     *
     * @param intent
     */
    protected void onDownloadedThumbnail(final Intent intent) {
        if (intent.getBooleanExtra(EXTRA_SUCCESS, false) && mAdapter != null) {
            mAdapter.update(intent.getStringExtra(EXTRA_FILE_ID));
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

    protected class BoxItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        BoxListItem mItem;

        View mView;
        ImageView mThumbView;
        TextView mNameView;
        TextView mMetaDescription;
        ProgressBar mProgressBar;

        public BoxItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mView = itemView;
            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mMetaDescription = (TextView) itemView.findViewById(R.id.metaline_description);
            mProgressBar = (ProgressBar) itemView.findViewById((R.id.spinner));
        }

        public void bindItem(BoxListItem item) {
            mItem = item;
            onBindBoxItemViewHolder(this);
        }

        public void setError(BoxListItem item) {
            mItem = item;
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
        public void onClick(View v) {
            if (mItem.getIsError()) {
                mItem.setIsError(false);
                mApiExecutor.execute(mItem.getTask());
                setLoading();
            }

            if (mListener != null) {
                if (mListener.handleOnItemClick(mItem.getBoxItem())) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    if (mItem.getBoxItem() instanceof BoxFolder) {
                        BoxFolder folder = (BoxFolder) mItem.getBoxItem();
                        FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();

                        // All fragments will always navigate into folders
                        BoxBrowseFolderFragment browseFolderFragment = BoxBrowseFolderFragment.newInstance(folder, mSession);
                        trans.replace(R.id.box_browsesdk_fragment_container, browseFolderFragment)
                                .addToBackStack(TAG)
                                .commit();
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
            if (item.getIsError()) {
                boxItemHolder.setError(item);
                return;
            } else if (item.getType() == BoxListItem.TYPE_FUTURE_TASK) {
                getApiExecutor().execute(item.getTask());
                boxItemHolder.setLoading();
                return;
            } else {
                boxItemHolder.bindItem(item);

                // Fetch thumbnails for media file types
                if (item.getBoxItem() instanceof BoxFile && isMediaType(item.getBoxItem().getName())) {
                    if (item.getTask() == null) {
                        item.setTask(downloadThumbnail(item.getBoxItem().getId(),
                                mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId()), boxItemHolder));
                    } else if (item.getTask().isDone()) {
                        try {
                            Intent intent = (Intent) item.getTask().get();
                            if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
                                // if we were unable to get this thumbnail for any reason besides a 404 try it again.
                                Object ex = intent.getSerializableExtra(EXTRA_EXCEPTION);
                                if (ex != null && ex instanceof BoxException && ((BoxException) ex).getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                                    item.setTask(downloadThumbnail(item.getBoxItem().getId(),
                                            mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId()), boxItemHolder));
                                }
                            }
                        } catch (Exception e) {
                            // e.printStackTrace();
                        }
                    }
                }
            }

            if (item.getTask() != null && !item.getTask().isDone()) {
                getThumbnailExecutor().execute(item.getTask());
            }
        }

        private boolean isMediaType(String name) {
            if (SdkUtils.isBlank(name)) {
                return false;
            }

            int index = name.lastIndexOf(".");
            if (index > 0) {
                String ext = name.substring(index + 1);
                return (ext.equals("gif") ||
                        ext.equals("bmp") ||
                        ext.equals("jpeg") ||
                        ext.equals("jpg") ||
                        ext.equals("png") ||
                        ext.equals("svg") ||
                        ext.equals("tiff"));
            }
            return false;
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
                boolean success = mListItems.remove(item);
            }
        }

        public void addAll(BoxListItems items) {
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

                listItem.setIsEnabled(isItemEnabled(listItem.getBoxItem()));
            }
            mListItems.add(listItem);
            mItemsMap.put(listItem.getIdentifier(), listItem);
        }

        public void update(String id) {
            BoxListItem item = mItemsMap.get(id);
            if (item != null) {
                int index = mListItems.indexOf(item);
                notifyItemChanged(index);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an item being tapped to be communicated to the activity
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called whenever an item in the RecyclerView is clicked
         *
         * @param item the item that was clicked
         * @return whether the click event should continue to be handled by the fragment
         */
        boolean handleOnItemClick(BoxItem item);
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
        String description = item.getModifiedAt() != null ?
                String.format(Locale.ENGLISH, "%s  • %s",
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(item.getModifiedAt()).toUpperCase(),
                        localFileSizeToDisplay(item.getSize())) :
                localFileSizeToDisplay(item.getSize());
        holder.getMetaDescription().setText(description);
        mThumbnailManager.setThumbnailIntoView(holder.getThumbView(), item);
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
    private FutureTask<Intent> downloadThumbnail(final String fileId, final File downloadLocation, final BoxItemViewHolder holder) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOADED_FILE_THUMBNAIL);
                intent.putExtra(EXTRA_FILE_ID, fileId);
                intent.putExtra(EXTRA_SUCCESS, false);
                try {
                    // no need to continue downloading thumbnail if we already have a thumbnail
                    if (downloadLocation.exists() && downloadLocation.length() > 0) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                        return intent;
                    }
                    // no need to continue downloading thumbnail if we are not viewing this thumbnail.
                    if (holder.getItem() == null || holder.getItem().getBoxItem() == null || !(holder.getItem().getBoxItem() instanceof BoxFile)
                            || !holder.getItem().getBoxItem().getId().equals(fileId)) {
                        intent.putExtra(EXTRA_SUCCESS, false);
                        return intent;
                    }

                    BoxApiFile api = new BoxApiFile(mSession);
                    DisplayMetrics metrics = getResources().getDisplayMetrics();
                    int thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_128;
                    if (metrics.density <= DisplayMetrics.DENSITY_MEDIUM) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_64;
                    } else if (metrics.density <= DisplayMetrics.DENSITY_HIGH) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_64;
                    }
                    api.getDownloadThumbnailRequest(downloadLocation, fileId)
                            .setMinHeight(thumbSize)
                            .setMinWidth(thumbSize)
                            .send();
                    if (downloadLocation.exists()) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                    }
                } catch (BoxException e) {
                    intent.putExtra(EXTRA_SUCCESS, false);
                    intent.putExtra(EXTRA_EXCEPTION, e);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
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

}
