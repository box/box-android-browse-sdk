package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.models.BoxItem;

import java.io.File;

public class BoxSearchAdapter extends BoxItemAdapter {

    public BoxSearchAdapter(Context context, BoxItemFilter boxItemFilter, BrowseController controller, OnInteractionListener listener) {
        super(context, boxItemFilter, controller, listener);
    }

    @Override
    public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
        return new SearchViewHolder(view);
    }

    class SearchViewHolder extends BoxItemViewHolder {
        public SearchViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder) {
            if (holder.getItem() == null || holder.getItem().getBoxItem() == null) {
                return;
            }
            final BoxItem item = holder.getItem().getBoxItem();
            holder.getNameView().setText(item.getName());
            holder.getMetaDescription().setText(BoxSearchListAdapter.createPath(item, File.separator));
            getThumbanilManager().setThumbnailIntoView(holder.getThumbView(), item);
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getMetaDescription().setVisibility(View.VISIBLE);
            holder.getThumbView().setVisibility(View.VISIBLE);
        }
    }
}
