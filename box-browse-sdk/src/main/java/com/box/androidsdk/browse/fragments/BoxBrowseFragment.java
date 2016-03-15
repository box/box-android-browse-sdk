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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIterator;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    public static final String ARG_LIMIT = "argLimit";

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
    private static final String EXTRA_TITLE = "BoxBrowseFragment.Title";
    private static final String EXTRA_CANCELED = "BoxBrowseFragment.Canceled";

    protected static final String EXTRA_SECONDARY_ACTION_LISTENER = "BoxBrowseFragment_SecondaryActionListener";
    protected static final String EXTRA_MULTI_SELECT_HANDLER = "BoxBrowseFragment_Multi_Select_Handler";


    private static List<String> THUMBNAIL_MEDIA_EXTENSIONS = Arrays.asList(new String[] {"gif", "jpeg", "jpg", "bmp", "svg", "png", "tiff"});

    protected String mUserId;
    protected BoxSession mSession;
    protected int mLimit = DEFAULT_LIMIT;

    private BoxIteratorItems mBoxIteratorItems;

    protected OnFragmentInteractionListener mListener;
    protected OnFragmentInteractionListener mSecondaryActionListener;
    protected MultiSelectHandler mMultiSelectHandler;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected ThumbnailManager mThumbnailManager;
    protected SwipeRefreshLayout mSwipeRefresh;

    protected LocalBroadcastManager mLocalBroadcastManager;
    private String mTitle;
    private boolean mWaitingForConnection;
    private boolean mIsConnected;


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
            mLimit = getArguments().getInt(ARG_LIMIT);
        }
        if (savedInstanceState != null) {
            setListItem((BoxIteratorItems) savedInstanceState.getSerializable(EXTRA_COLLECTION));
            if (savedInstanceState.containsKey(EXTRA_SECONDARY_ACTION_LISTENER)){
                mSecondaryActionListener = (OnFragmentInteractionListener)savedInstanceState.getSerializable(EXTRA_SECONDARY_ACTION_LISTENER);
            }
            if (savedInstanceState.containsKey(EXTRA_MULTI_SELECT_HANDLER)){
                mMultiSelectHandler = (MultiSelectHandler)savedInstanceState.getSerializable(EXTRA_MULTI_SELECT_HANDLER);
            }
        }
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, initializeIntentFilters());
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
            outState.putSerializable(EXTRA_SECONDARY_ACTION_LISTENER, (Serializable)mSecondaryActionListener);
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
            return new ThumbnailManager(getActivity().getCacheDir());
        } catch (FileNotFoundException e) {
            // TODO: Call error handler
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
        if (getMultiSelectHandler() != null){
            getMultiSelectHandler().setItemAdapter(mAdapter);
        }

        if (mBoxIteratorItems == null) {
            mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        } else {
            displayBoxList(mBoxIteratorItems);

        }
        return rootView;
    }

    protected void setListItem(final BoxIteratorItems items) {
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
        getApiExecutor().execute(fetchInfo());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    protected OnFragmentInteractionListener getSecondaryActionListener(){
        return mSecondaryActionListener;
    }

    /**
     * Optionally set a secondary action to use on this fragment.
     * @param listener listener to be called when a secondary action is clicked on an item. Must be serializable.
     * @param <T> Serializable OnFragmentInteractionListener.
     */
    public <T extends OnFragmentInteractionListener & Serializable> void setSecondaryActionListener(T listener){
        mSecondaryActionListener = listener;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     *
     * @return the MultiSelectHandler set on this fragment. This is used in order to handle batch operations.
     */
    public MultiSelectHandler getMultiSelectHandler(){
        return mMultiSelectHandler;
    }

    /**
     * Optionally set a multi select handler.
     * @param handler handler to be called when multiple items are selected.
     * @param <T> Serializable OnFragmentInteractionListener.
     */
    public <T extends MultiSelectHandler & Serializable> void setMultiSelectHandler(T handler){
        mMultiSelectHandler = handler;
        if (mAdapter != null) {
            mMultiSelectHandler.setItemAdapter(mAdapter);
        }
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
            checkConnectivity();
            return;
        }
        checkConnectivity();
        displayBoxList((BoxIteratorItems) intent.getSerializableExtra(EXTRA_COLLECTION));
        mSwipeRefresh.setRefreshing(false);
    }

    /**
     * Call on loading error and refresh if loss of connectivity is the suspect.
     */
    protected void checkConnectivity() {
        mWaitingForConnection = !mIsConnected;
    }
    /**
     * show in this fragment a box list of items.
     */
    protected void displayBoxList(final BoxIteratorItems items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        // if we are trying to display the original list no need to add.
        if (items == mBoxIteratorItems) {

            //  mBoxIteratorItems.addAll(items);
            if (mAdapter.getItemCount() < 1) {
                mAdapter.addAll(items);
            }
        } else {
            if (mBoxIteratorItems == null) {
                setListItem(items);
            }

            addAllItems(mBoxIteratorItems, items);
            mAdapter.addAll(items);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

        if (items.fullSize() != null && mBoxIteratorItems.size() < items.fullSize()) {
            // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
            mAdapter.add(new BoxListItem(fetchItems(mBoxIteratorItems.size(), mLimit), ACTION_FETCHED_ITEMS));
        }

    }

    private BoxIteratorItems addAllItems(BoxIteratorItems target, BoxIteratorItems source){
        JsonValue sourceArray = source.toJsonObject().get(BoxIterator.FIELD_ENTRIES);
        JsonObject targetJsonObject = target.toJsonObject();
        JsonValue targetArray = targetJsonObject.get(BoxIterator.FIELD_ENTRIES);
        if (targetArray == null || targetArray.isNull()){
            JsonArray jsonArray = new JsonArray();
            targetJsonObject.set(BoxIterator.FIELD_ENTRIES, jsonArray);
            target.createFromJson(targetJsonObject);
            targetArray = jsonArray;
        }
        if (sourceArray != null) {
            for (JsonValue value : sourceArray.asArray()) {
                targetArray.asArray().add(value);
            }
        }
        return target;

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

    protected class BoxItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        BoxListItem mItem;

        View mView;
        ImageView mThumbView;
        TextView mNameView;
        TextView mMetaDescription;
        ProgressBar mProgressBar;
        ImageButton mSecondaryAction;
        BoxItemClickListener mSecondaryClickListener;
        CheckBox mItemCheckBox;


        public BoxItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            if (getMultiSelectHandler() != null){
                itemView.setOnLongClickListener(this);
            }
            mView = itemView;
            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mMetaDescription = (TextView) itemView.findViewById(R.id.metaline_description);
            mProgressBar = (ProgressBar) itemView.findViewById((R.id.spinner));
            mSecondaryAction = (ImageButton) itemView.findViewById(R.id.secondaryAction);
            mItemCheckBox = (CheckBox)itemView.findViewById(R.id.boxItemCheckBox);
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
            if (getMultiSelectHandler() != null){
                getMultiSelectHandler().toggle(mItem.getBoxItem());
                getMultiSelectHandler().setEnabled(!getMultiSelectHandler().isEnabled());
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if (getMultiSelectHandler() != null && getMultiSelectHandler().isEnabled()){
                getMultiSelectHandler().toggle(mItem.getBoxItem());
                onBindBoxItemViewHolder(this);
                return;
            }
            if(mSwipeRefresh.isRefreshing()){
                return;
            }
            if (mItem == null) {
                return;
            }

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

    protected  class BoxItemAdapter extends RecyclerView.Adapter<BoxItemViewHolder> {
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
                    if (item.getTask() == null ) {
                        item.setTask(downloadThumbnail(item.getBoxItem().getId(),
                                mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId()), boxItemHolder));
                    } else if (item.getTask().isDone()) {
                        try {
                            Intent intent = (Intent) item.getTask().get();
                            boolean canceled = intent.getBooleanExtra(EXTRA_CANCELED, false);
                            if (intent.getBooleanExtra(EXTRA_SUCCESS, true) || canceled) {
                                // if we were unable to get this thumbnail for any reason besides a 404 try it again.
                                Object ex = intent.getSerializableExtra(EXTRA_EXCEPTION);
                                if (canceled || ex != null && ex instanceof BoxException && ((BoxException) ex).getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
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
                return THUMBNAIL_MEDIA_EXTENSIONS.contains(name.substring(index + 1).toLowerCase());
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

    private class BoxItemClickListener implements View.OnClickListener {

        protected BoxListItem mBoxListItem;

        void setListItem(BoxListItem listItem){
            mBoxListItem = listItem;
        }

        @Override
        public void onClick(View v) {
            getSecondaryActionListener().handleOnItemClick(mBoxListItem.getBoxItem());
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


    public static abstract class MultiSelectHandler {


        HashSet<BoxItem> mSelectedItems = new HashSet<BoxItem>();
        boolean mIsMultiSelecting;
        transient WeakReference<BoxItemAdapter> mItemAdapter;

        public List<BoxItem> getSelectedBoxItem(){
            ArrayList<BoxItem> items = new ArrayList<BoxItem>(mSelectedItems.size());
            mSelectedItems.addAll(items);
            return items;
        }

        public int getSize(){
            return mSelectedItems.size();
        }

        public boolean isItemSelected(BoxItem item){
            boolean selected = mSelectedItems.contains(item);
            return mSelectedItems.contains(item);
        }

        /**
         *
         * @param boxItem box item the user may potentially select.
         * @return true if this item can be selected by the user, false if it should be disabled.
         */
        public abstract boolean isSelectable(BoxItem boxItem);

        /**
         * Called when a user selects or deselects an item in the list.
         * @param boxItem box item the user clicked while in multi select mode.
         * @param wasSelected whether or not the the item the user clicked was selected.
         * @param handler the handler that keeps track of which files were chosen.
         */
        public abstract void handleItemSelected(BoxItem boxItem, boolean wasSelected, MultiSelectHandler handler);

        void toggle(BoxItem boxItem){
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

        void setItemAdapter(BoxItemAdapter adapter){
            mItemAdapter = new WeakReference<BoxItemAdapter>(adapter);
        }


        boolean isEnabled(){
            return mIsMultiSelecting;
        }

        public void setEnabled(boolean enabled){
            if (mIsMultiSelecting == enabled){
                return;
            }
            mIsMultiSelecting = enabled;
            if (enabled == false){
                mSelectedItems.clear();
            }
            if (mItemAdapter != null && mItemAdapter.get() != null) {
                mItemAdapter.get().notifyDataSetChanged();
            }
        }

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
        if (getSecondaryActionListener() != null){
            holder.getSecondaryAction().setVisibility(View.VISIBLE);
        } else {
            holder.getSecondaryAction().setVisibility(View.GONE);

        }

        if (getMultiSelectHandler() != null && getMultiSelectHandler().isEnabled()){
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
                        intent.putExtra(EXTRA_CANCELED, true);
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


}
