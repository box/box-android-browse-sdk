package com.box.androidsdk.browse.adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.support.v4.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.uidata.NavigationItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxList;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for navigation items.
 * 
 */
public class BoxSearchListAdapter extends ResourceCursorAdapter implements BoxFutureTask.OnCompletedListener<BoxListItems>{


    private static BoxFutureTask<BoxList> mSearchTask;
    private static ThreadPoolExecutor mApiExecutor;
    private Handler mHandler;
    private ThumbnailManager mThumbnailManager;
    private OnBoxSearchListener mOnBoxSearchListener;
    public static int DEFAULT_MAX_SUGGESTIONS = 9;

    public BoxSearchListAdapter(Context context, int layout, int flags, final BoxSession session){
        super(context, layout, new BoxSearchCursor(null, ""), flags);
        mHandler = new Handler(Looper.getMainLooper());
        try {
            mThumbnailManager = new ThumbnailManager(context.getCacheDir());
        } catch (Exception e){

        }
    }

    @Override
    public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
        if (constraint != null) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (getCursor() != null) {
                        changeCursor(new BoxSearchCursor(getCursor().getBoxList(), constraint.toString()));
                    } else {
                        changeCursor(new BoxSearchCursor(null, constraint.toString()));
                    }
                }
            });
        }

        return super.runQueryOnBackgroundThread(constraint);
    }


    public void setOnBoxSearchListener(final OnBoxSearchListener searchListener){
        mOnBoxSearchListener = searchListener;
    }

    private Handler getUiHandler(){
        return mHandler;
    }

    public BoxSearchCursor getCursor(){
        return (BoxSearchCursor)super.getCursor();
    }

    protected ThreadPoolExecutor getApiExecutor(){
        if (mApiExecutor == null){
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r);
                        }
                    });
        }
        return mApiExecutor;
    }

    @Override
    public boolean isEnabled(final int position) {
        return true;
    }

    @Override
    public void onCompleted(BoxResponse<BoxListItems> boxListBoxResponse) {
        if (boxListBoxResponse.isSuccess()) {

            Cursor cursor = new BoxSearchCursor(boxListBoxResponse.getResult());
            changeCursor(cursor);
        }

    }

    /**
     * The Executor used for thumbnail api calls.
     */
    private ThreadPoolExecutor thumbnailApiExecutor;

    /**
     * Executor that we will submit thumbnail tasks to.
     *
     * @return executor
     */
    protected ThreadPoolExecutor getThumbnailApiExecutor() {
        if (thumbnailApiExecutor == null || thumbnailApiExecutor.isShutdown()) {
            thumbnailApiExecutor = new ThreadPoolExecutor(1, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return thumbnailApiExecutor;
    }

    /* Download the thumbnail for a given file.
    *
            * @param fileId file id to download thumbnail for.
            * @return A FutureTask that is tasked with fetching information on the given folder.
            */
    public java.util.concurrent.FutureTask<Intent> downloadThumbnail(final BoxApiFile fileApi, final String fileId, final File downloadLocation, final ViewHolder holder) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();

                try {
                    // no need to continue downloading thumbnail if we already have a thumbnail
                    if (downloadLocation.exists() && downloadLocation.length() > 0) {
                        return intent;
                    }
                    // no need to continue downloading thumbnail if we are not viewing this thumbnail.
                    if (holder.boxItem == null || !(holder.boxItem instanceof BoxFile)
                            || !holder.boxItem.getId().equals(fileId)) {
                        return intent;
                    }
                    DisplayMetrics metrics = holder.icon.getResources().getDisplayMetrics();
                    int thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_128;
                    if (metrics.density <= DisplayMetrics.DENSITY_MEDIUM) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_64;
                    } else if (metrics.density <= DisplayMetrics.DENSITY_HIGH) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_64;
                    }
                    BoxDownload download = fileApi.getDownloadThumbnailRequest(downloadLocation, fileId)
                            .setMinHeight(thumbSize)
                            .setMinWidth(thumbSize).send();
                    if (downloadLocation.exists()) {
                        if (holder.boxItem == null || !(holder.boxItem instanceof BoxFile)
                                || !holder.boxItem.getId().equals(fileId)) {
                            return intent;
                        }
                        else {
                            mThumbnailManager.setThumbnailIntoView(holder.icon, holder.boxItem);
                        }
                    }
                } catch (BoxException e) {

                }

                return intent;
            }
        });

    }

    protected BoxApiFile getFileApi(){
        if (getFilterQueryProvider() instanceof SearchFilterQueryProvider){
            return ((SearchFilterQueryProvider) getFilterQueryProvider()).getFileApi();
        }
        return null;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder)view.getTag();
            if (holder != null){
                holder.name.setText(((BoxSearchCursor)cursor).getName());
                try{
                    if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_NORMAL)
                        ((BoxSearchCursor) cursor).getBoxItem();
                } catch (Exception e){
                    if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_NORMAL)
                    return;
                }
                if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_NORMAL) {
                    holder.description.setText(((BoxSearchCursor) cursor).getPath());
                    BoxItem item = ((BoxSearchCursor) cursor).getBoxItem();
                    holder.boxItem = item;
                    mThumbnailManager.setThumbnailIntoView(holder.icon, item);
                    if (item instanceof BoxFile) {
                        if (getFileApi() != null) {
                            getThumbnailApiExecutor().execute(downloadThumbnail(getFileApi(),item.getId(),mThumbnailManager.getThumbnailForFile(item.getId()), holder));
                        }
                    }
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.icon.setVisibility(View.VISIBLE);
                } else  if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_QUERY) {
                    holder.boxItem = null;
                    holder.description.setText(R.string.box_browsesdk_performing_search);
                    holder.icon.setImageResource(R.drawable.ic_box_browsesdk_search_grey_24dp);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.icon.setVisibility(View.INVISIBLE);
                } else if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_ADDITIONAL_RESULT){
                    holder.boxItem = null;
                    holder.name.setText(R.string.box_browsesdk_see_additional_results);
                    holder.description.setText("");
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.icon.setVisibility(View.INVISIBLE);
                }
            }
    }

    public void setSession(final BoxSession session){
        if (session == null){
            setFilterQueryProvider(null);
        } else {
            setFilterQueryProvider(new SearchFilterQueryProvider(session));
        }


    }



    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.box_browsesdk_list_item, parent, false);
        ViewHolder vh = new ViewHolder();
        vh.icon = (ImageView)v.findViewById(R.id.box_browsesdk_thumb_image);
        vh.name = (TextView)v.findViewById(R.id.box_browsesdk_name_text);
        vh.description = (TextView)v.findViewById(R.id.metaline_description);
        vh.progressBar = (ProgressBar)v.findViewById(R.id.spinner);
        v.setTag(vh);

        return v;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView description;
        ProgressBar progressBar;
        BoxItem boxItem;


    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        // not used.
        return null;
    }


    private class SearchFilterQueryProvider implements FilterQueryProvider {
        private final BoxApiSearch mSearchApi;
        private final BoxApiFile mFileApi;

        public SearchFilterQueryProvider(final BoxSession session){
            mSearchApi = new BoxApiSearch(session);
            mFileApi = new BoxApiFile(session);
        }

        public BoxApiSearch getSearchApi(){
            return mSearchApi;
        }

        public BoxApiFile getFileApi(){
            return mFileApi;
        }

        @Override
        public Cursor runQuery(CharSequence constraint) {
            if (constraint == null){
                return null;
            }
            BoxRequestsSearch.Search search = mSearchApi.getSearchRequest(constraint.toString());
            search = onSearchRequested(search);
            try {

                if (search != null) {
                    return new BoxSearchCursor(search.send());
                }
            } catch (BoxException e){
              //  e.printStackTrace();
            }
            return new BoxSearchCursor(null, "failed: " + constraint);
        }

    }

    public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest){
        if (mOnBoxSearchListener != null){
            return mOnBoxSearchListener.onSearchRequested(searchRequest);
        }
        return searchRequest;
    }

    /**
     * Used to listen to particular events related to this adapter.
     */
    public static interface OnBoxSearchListener {

        /**
         * This is called before any search calls are sent. Allows modification of search request
         * adding limitations, etc...
         * @param searchRequest The most basic search request that searches entire account.
         * @return the search request desired to be performed, or null to not perform a search.
         */
        public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest);


    }


    public static String createPath(final BoxItem boxItem, final String separator){
        StringBuilder builder = new StringBuilder(separator);
        if (boxItem.getPathCollection() != null) {
            for (BoxFolder folder : boxItem.getPathCollection()) {
                builder.append(folder.getName());
                builder.append(separator);
            }
        }
        return builder.toString();

    }

    public static class BoxSearchCursor extends MatrixCursor {
        public static int TYPE_NORMAL = 0;
        public static int TYPE_QUERY  = 1;
        public static int TYPE_ADDITIONAL_RESULT = 2;

        private static final String[] SEARCH_COLUMN_NAMES = new String[]{"_id", "name", "path", "type"};
        private final BoxListItems mBoxList;

        BoxSearchCursor(final BoxListItems boxList){
            super(SEARCH_COLUMN_NAMES, boxList.size());
            mBoxList = boxList;
            initializeFromList(boxList);
            addRow(new Object[]{-2, "","", TYPE_ADDITIONAL_RESULT});
        }

        BoxSearchCursor(final BoxListItems boxList, final String query){
            super(SEARCH_COLUMN_NAMES);
            mBoxList = boxList;
            addRow(new Object[]{"-1",  query,"", TYPE_QUERY});
            initializeFromList(boxList);
        }

        public BoxItem getBoxItem(){
            return (BoxItem)mBoxList.get(mPos);
        }

        protected void initializeFromList(final BoxListItems boxList){
            if (boxList == null){
                return;
            }
            int i = 0;
            for (BoxJsonObject item : boxList){
                if (i >= DEFAULT_MAX_SUGGESTIONS){
                    break;
                }
                if (item instanceof BoxItem){
                    addRow(new Object[]{((BoxItem) item).getId(), ((BoxItem) item).getName(), createPath((BoxItem)item, File.separator), TYPE_NORMAL});
                }
                i++;
            }
        }

        public int getType(){
            return this.getInt(getColumnIndex("type"));
        }

        public BoxListItems getBoxList(){
            return mBoxList;
        }

        public String getName(){
            return this.getString(getColumnIndex("name"));
        }

        public String getPath(){
            return this.getString(getColumnIndex("path"));
        }

    }

}
