package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.eclipsesource.json.JsonObject;

import java.io.File;

public class BoxSearchAdapter extends BoxItemAdapter {

    public static final String LOAD_MORE_ID = "com.box.androidsdk.browse.LOAD_MORE";
    protected static final int LOAD_MORE_VIEW_TYPE = 1;

    public BoxSearchAdapter(Context context, BrowseController controller, OnInteractionListener listener) {
        super(context, controller, listener);
    }

    @Override
    public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
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
        if (item instanceof LoadMoreItem) {
            return LOAD_MORE_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    public void addLoadMoreItem(BoxRequestsSearch.Search searchReq) {
        this.add(LoadMoreItem.create(searchReq));
    }

    class SearchViewHolder extends BoxItemViewHolder {
        public SearchViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            if (itemToBind == null || itemToBind == null) {
                return;
            }

            holder.getNameView().setText(itemToBind.getName());
            holder.getMetaDescription().setText(BoxSearchListAdapter.createPath(itemToBind, File.separator));
            mController.getThumbnailManager().loadThumbnail(itemToBind, holder.getThumbView());
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getMetaDescription().setVisibility(View.VISIBLE);
            holder.getThumbView().setVisibility(View.VISIBLE);
        }
    }

    class LoadMoreViewHolder extends BoxItemViewHolder {
        public LoadMoreViewHolder(View view) {
            super(view);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            mController.execute(((LoadMoreItem) mItem).getRequest());
        }

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

    static LoadMoreItem create(BoxRequestsSearch.Search request) {
        JsonObject object = new JsonObject();
        object.add(BoxItem.FIELD_ID, BoxSearchAdapter.LOAD_MORE_ID);
        LoadMoreItem ret = new LoadMoreItem(object);
        ret.setRequest(request);
        return ret;
    }

    public BoxRequestsSearch.Search getRequest() {
        return mRequest;
    }

    private void setRequest(BoxRequestsSearch.Search request) {
        mRequest = request;
    }
}
