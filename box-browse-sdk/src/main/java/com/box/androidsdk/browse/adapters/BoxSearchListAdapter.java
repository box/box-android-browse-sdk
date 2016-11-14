package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for navigation items.
 */
public class BoxSearchListAdapter extends ResourceCursorAdapter implements BoxFutureTask.OnCompletedListener<BoxIteratorItems>{


    private static final String TAG = BoxSearchListAdapter.class.getName();
    private Handler mHandler;
    private OnBoxSearchListener mOnBoxSearchListener;
    public static int DEFAULT_MAX_SUGGESTIONS = 9;

    /**
     * Instantiates a new Box search list adapter.
     *
     * @param context the context
     * @param layout  the layout
     * @param flags   the flags
     */
    public BoxSearchListAdapter(Context context, int layout, int flags){
        super(context, layout, new BoxSearchCursor(null, ""), flags);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
        if (constraint != null) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (getCursor() != null) {
                        changeCursor(new BoxSearchCursor(getCursor().getBoxIterator(), constraint.toString()));
                    } else {
                        changeCursor(new BoxSearchCursor(null, constraint.toString()));
                    }
                }
            });
        }

        return super.runQueryOnBackgroundThread(constraint);
    }


    /**
     * Set on box search listener.
     *
     * @param searchListener the search listener
     */
    public void setOnBoxSearchListener(final OnBoxSearchListener searchListener){
        mOnBoxSearchListener = searchListener;
    }

    private Handler getUiHandler(){
        return mHandler;
    }

    public BoxSearchCursor getCursor(){
        return (BoxSearchCursor)super.getCursor();
    }

    @Override
    public boolean isEnabled(final int position) {
        return true;
    }

    @Override
    public void onCompleted(BoxResponse<BoxIteratorItems> BoxIteratorBoxResponse) {
        if (BoxIteratorBoxResponse.isSuccess()) {

            Cursor cursor = new BoxSearchCursor(BoxIteratorBoxResponse.getResult());
            changeCursor(cursor);
        }

    }

    /**
     * Gets controller.
     *
     * @return the controller
     */
    protected BrowseController getController() {
        if (getFilterQueryProvider() instanceof SearchFilterQueryProvider) {
            return ((SearchFilterQueryProvider) getFilterQueryProvider()).getController();
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
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.thumbnail.setVisibility(View.VISIBLE);
                    getController().getThumbnailManager().loadThumbnail(item, holder.thumbnail);
                } else  if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_QUERY) {
                    holder.boxItem = null;
                    holder.description.setText(R.string.box_browsesdk_performing_search);
                    holder.thumbnail.setImageResource(R.drawable.ic_box_browsesdk_search_grey_24dp);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.thumbnail.setVisibility(View.INVISIBLE);
                } else if (((BoxSearchCursor)cursor).getType() == BoxSearchCursor.TYPE_ADDITIONAL_RESULT){
                    holder.boxItem = null;
                    holder.name.setText(R.string.box_browsesdk_see_additional_results);
                    holder.description.setText("");
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.thumbnail.setVisibility(View.INVISIBLE);
                }
            }
    }

    /**
     * Set controller.
     *
     * @param controller the controller
     */
    public void setController(final BrowseController controller){
        if (controller == null){
            setFilterQueryProvider(null);
        } else {
            setFilterQueryProvider(new SearchFilterQueryProvider(controller));
        }


    }



    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.box_browsesdk_list_item, parent, false);
        ViewHolder vh = new ViewHolder();
        vh.thumbnail = (ImageView)v.findViewById(R.id.box_browsesdk_thumb_image);
        vh.name = (TextView)v.findViewById(R.id.box_browsesdk_name_text);
        vh.description = (TextView)v.findViewById(R.id.metaline_description);
        vh.progressBar = (ProgressBar)v.findViewById(R.id.spinner);
        v.setTag(vh);

        return v;
    }

    private static class ViewHolder {

        ImageView thumbnail;
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
        private final BrowseController mController;

        /**
         * Instantiates a new Search filter query provider.
         *
         * @param controller the controller
         */
        public SearchFilterQueryProvider(final BrowseController controller){
            mController = controller;
        }

        @Override
        public Cursor runQuery(CharSequence constraint) {
            if (constraint == null){
                return null;
            }
            BoxRequestsSearch.Search search = mController.getSearchRequest(constraint.toString());
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

        /**
         * Gets controller.
         *
         * @return the controller
         */
        public BrowseController getController() {
            return mController;
        }
    }

    /**
     * On search requested
     *
     * @param searchRequest the search request
     * @return updated box search request
     */
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
         *
         * @param searchRequest The most basic search request that searches entire account.
         * @return the search request desired to be performed, or null to not perform a search.
         */
        public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest);


    }


    /**
     * Create path string.
     *
     * @param boxItem   the box item
     * @param separator the separator
     * @return the string that represents a path to the boxItem
     */
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

    /**
     * The type Box search cursor.
     */
    public static class BoxSearchCursor extends MatrixCursor {

        public static int TYPE_NORMAL = 0;
        public static int TYPE_QUERY  = 1;
        public static int TYPE_ADDITIONAL_RESULT = 2;

        private static final String[] SEARCH_COLUMN_NAMES = new String[]{"_id", "name", "path", "type"};
        private final BoxIteratorItems mBoxIterator;

        /**
         * Instantiates a new Box search cursor.
         *
         * @param BoxIterator the box iterator
         */
        BoxSearchCursor(final BoxIteratorItems BoxIterator){
            super(SEARCH_COLUMN_NAMES, BoxIterator.size());
            mBoxIterator = BoxIterator;
            initializeFromList(BoxIterator);
            if (BoxIterator != null && BoxIterator.size() >= BoxSearchListAdapter.DEFAULT_MAX_SUGGESTIONS) {
                addRow(new Object[]{-2, "","", TYPE_ADDITIONAL_RESULT});
            }
        }

        /**
         * Instantiates a new Box search cursor.
         *
         * @param BoxIterator the box iterator
         * @param query       the search query
         */
        BoxSearchCursor(final BoxIteratorItems BoxIterator, final String query){
            super(SEARCH_COLUMN_NAMES);
            mBoxIterator = BoxIterator;
            addRow(new Object[]{"-1",  query,"", TYPE_QUERY});
            initializeFromList(BoxIterator);
        }

        /**
         * Get box item box item.
         *
         * @return the box item
         */
        public BoxItem getBoxItem(){
            return (BoxItem)mBoxIterator.get(getPosition());
        }

        /**
         * Initialize from list.
         *
         * @param BoxIterator the box iterator
         */
        protected void initializeFromList(final BoxIteratorItems BoxIterator){
            if (BoxIterator == null){
                return;
            }
            int i = 0;
            for (BoxJsonObject item : BoxIterator){
                if (i >= DEFAULT_MAX_SUGGESTIONS){
                    break;
                }
                if (item instanceof BoxItem){
                    addRow(new Object[]{((BoxItem) item).getId(), ((BoxItem) item).getName(), createPath((BoxItem)item, File.separator), TYPE_NORMAL});
                }
                i++;
            }
        }

        /**
         * Get type int.
         *
         * @return the int
         */
        public int getType(){
            return this.getInt(getColumnIndex("type"));
        }

        /**
         * Get box iterator box iterator items.
         *
         * @return the box iterator items
         */
        public BoxIteratorItems getBoxIterator(){
            return mBoxIterator;
        }

        /**
         * Get name string.
         *
         * @return the string
         */
        public String getName(){
            return this.getString(getColumnIndex("name"));
        }

        /**
         * Get path string.
         *
         * @return the string
         */
        public String getPath(){
            return this.getString(getColumnIndex("path"));
        }

    }

}
