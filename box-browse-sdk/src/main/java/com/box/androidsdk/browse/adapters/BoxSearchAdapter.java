package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.eclipsesource.json.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Box search adapter.
 */
public class BoxSearchAdapter extends BoxItemAdapter {

    public static final String LOAD_MORE_ID = "com.box.androidsdk.browse.LOAD_MORE";
    protected static final int LOAD_MORE_VIEW_TYPE = 1;
    protected static final int RESULTS_HEADER_VIEW_TYPE = 2;

    /**
     * Instantiates a new Box search adapter.
     *
     * @param context    the context
     * @param controller BrowseController instance
     * @param listener   OnInteractionListener to net notified for any interactions with the items
     */
    public BoxSearchAdapter(Context context, BrowseController controller, OnInteractionListener listener) {
        super(context, controller, listener);
    }

    @Override
    public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case RESULTS_HEADER_VIEW_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_recent_searches_header, viewGroup, false);
                return new ResultsHeaderViewHolder(view);
            case LOAD_MORE_VIEW_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_load_more_item, viewGroup, false);
                return new LoadMoreViewHolder(view);
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
                return new SearchViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        BoxItem item = mItems.get(position);

        if (item instanceof ResultsHeader) {
            return RESULTS_HEADER_VIEW_TYPE;
        }

        if (item instanceof LoadMoreItem) {
            return LOAD_MORE_VIEW_TYPE;
        }

        return super.getItemViewType(position);
    }

    /**
     * Add load more item.
     *
     * @param searchReq the search req which will be executed once the load more item is visible
     */
    public void addLoadMoreItem(BoxRequestsSearch.Search searchReq) {
        ArrayList<BoxItem> list = new ArrayList<BoxItem>(1);
        list.add(LoadMoreItem.create(searchReq));
        this.add(list);
    }

    /**
     * The type Search view holder.
     */
    class SearchViewHolder extends BoxItemViewHolder {
        /**
         * Instantiates a new Search view holder.
         *
         * @param itemView view to bind with this holder
         */
        public SearchViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            if (itemToBind == null || itemToBind == null) {
                return;
            }

            super.onBindBoxItemViewHolder(holder, itemToBind);
            holder.getNameView().setText(itemToBind.getName());
            holder.getMetaDescription().setText(BoxSearchListAdapter.createPath(itemToBind, File.separator));
            mController.getThumbnailManager().loadThumbnail(itemToBind, holder.getThumbView());
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getMetaDescription().setVisibility(View.VISIBLE);
            holder.getThumbView().setVisibility(View.VISIBLE);
        }
    }

    /**
     * The type Load more view holder.
     */
    class LoadMoreViewHolder extends BoxItemViewHolder {
        /**
         * Instantiates a new Load more view holder.
         *
         * @param view the view to bind with this holder
         */
        public LoadMoreViewHolder(View view) {
            super(view);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            mController.execute(((LoadMoreItem) itemToBind).getRequest());
        }

        /**
         * Sets error.
         */
        public void setError() {
            // TODO: Should set error state
            mThumbView.setImageResource(R.drawable.ic_box_browsesdk_refresh_grey_36dp);
            mThumbView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mMetaDescription.setVisibility(View.VISIBLE);
            mNameView.setText(mContext.getResources().getString(R.string.box_browsesdk_error_retrieving_items));
            mMetaDescription.setText(mContext.getResources().getString(R.string.box_browsesdk_tap_to_retry));
        }

    }

    /**
     * The type Results header view holder.
     */
    class ResultsHeaderViewHolder extends BoxItemViewHolder {
        /**
         * Instantiates a new Results header view holder.
         *
         * @param view the view
         */
        public ResultsHeaderViewHolder(View view) {
            super(view);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            ((TextView)holder.getView().findViewById(R.id.text)).setText(Html.fromHtml(String.format(mContext.getResources().getString(R.string.box_browsesdk_search_results_header), itemToBind.getName())));
        }
    }
}

/**
 * Fake item used to notify that the user has scrolled to the bottom and more results should be fetched.
 * Package protected as Search is currently the only fragment that requires incremental loading since
 * there can potentially be a large set of results
 */
class LoadMoreItem extends BoxItem {

    private BoxRequestsSearch.Search mRequest;

    private LoadMoreItem(JsonObject obj) {
        super(obj);
    }

    /**
     * Create load more item.
     *
     * @param request the request
     * @return the load more item
     */
    static LoadMoreItem create(BoxRequestsSearch.Search request) {
        JsonObject object = new JsonObject();
        object.add(BoxItem.FIELD_ID, BoxSearchAdapter.LOAD_MORE_ID);
        LoadMoreItem ret = new LoadMoreItem(object);
        ret.setRequest(request);
        return ret;
    }

    /**
     * Gets request.
     *
     * @return the request
     */
    public BoxRequestsSearch.Search getRequest() {
        return mRequest;
    }

    private void setRequest(BoxRequestsSearch.Search request) {
        mRequest = request;
    }
}

