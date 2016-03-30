package com.box.androidsdk.browse.adapters;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.activities.BoxBrowseActivity;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class BoxItemAdapter extends RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder> {
    private final Context mContext;
    private final BrowseController mController;
    private final OnInteractionListener mListener;
    protected ArrayList<BoxListItem> mListItems = new ArrayList<BoxListItem>();
    protected HashMap<String, BoxListItem> mItemsMap = new HashMap<String, BoxListItem>();
    private BoxItemFilter mBoxItemFilter;
    private ThumbnailManager mThumbnailManager;

    protected int ITEM_VIEW_TYPE = 1;

    public BoxItemAdapter(Context context, BoxItemFilter boxItemFilter, BrowseController controller, OnInteractionListener listener) {
        mContext = context;
        mBoxItemFilter = boxItemFilter;
        mController = controller;
        mListener = listener;
    }


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
                BoxRequestsFile.DownloadThumbnail req = mController.getThumbnailRequest(item.getBoxItem().getId(),
                        getThumbanilManager().getThumbnailForFile(item.getBoxItem().getId()), mContext.getResources());
                item.setRequest(req);
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
            mController.execute(item.getRequest());
            if (item.getIdentifier().equals(BoxBrowseFragment.ACTION_FUTURE_TASK)) {
                boxItemHolder.setLoading();
            }
            return;
        }
    }

    @Override
    public int getItemCount() {
        return mListItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
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
            //Filter out item if necessary
            if (mBoxItemFilter != null && !mBoxItemFilter.accept(listItem.getBoxItem())) {
                return;
            }
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

    protected ThumbnailManager getThumbanilManager() {
        if (mThumbnailManager == null) {
            try {
                mThumbnailManager = new ThumbnailManager(mController.getThumbnailCacheDir());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return mThumbnailManager;
    }

    public List<BoxListItem> getItems() {
        return mListItems;
    }


    public class BoxItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
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
            if (mListener.getMultiSelectHandler() != null) {
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
            setAccentColor(mContext.getResources(), mProgressBar);
        }

        public void bindItem(BoxListItem item) {
            mItem = item;
            mSecondaryClickListener.setListItem(item);
            onBindBoxItemViewHolder(this);
        }


        /**
         * Called when a {@link BoxListItem} is bound to a ViewHolder. Customizations of UI elements
         * should be done by overriding this method. If extending from a {@link BoxBrowseActivity}
         * a custom BoxBrowseFolder fragment can be returned in
         * {@link BoxBrowseActivity#createBrowseFolderFragment(BoxItem, BoxSession)}
         *
         * @param holder the BoxItemHolder
         */
        protected void onBindBoxItemViewHolder(BoxItemAdapter.BoxItemViewHolder holder) {
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
                getThumbanilManager().setThumbnailIntoView(holder.getThumbView(), item);
            }
            holder.getMetaDescription().setText(description);
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getMetaDescription().setVisibility(View.VISIBLE);
            holder.getThumbView().setVisibility(View.VISIBLE);
            if (!holder.getItem().getIsEnabled()) {
                holder.getView().setEnabled(false);
                holder.getNameView().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_hint));
                holder.getMetaDescription().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_disabled_hint));
                holder.getThumbView().setAlpha(0.26f);
            } else {
                holder.getView().setEnabled(true);
                holder.getNameView().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_primary_text));
                holder.getMetaDescription().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_hint));
                holder.getThumbView().setAlpha(1f);
            }
            if (mListener.getOnSecondaryActionListener() != null) {
                holder.getSecondaryAction().setVisibility(View.VISIBLE);
            } else {
                holder.getSecondaryAction().setVisibility(View.GONE);
            }

            if (mListener.getMultiSelectHandler() != null && mListener.getMultiSelectHandler().isEnabled()) {
                holder.getSecondaryAction().setVisibility(View.GONE);
                holder.getCheckBox().setVisibility(View.VISIBLE);
                holder.getCheckBox().setEnabled(mListener.getMultiSelectHandler().isSelectable(item));
                holder.getCheckBox().setChecked(mListener.getMultiSelectHandler().isItemSelected(item));
            } else {
                holder.getCheckBox().setVisibility(View.GONE);
            }

        }

        public void setError(BoxListItem item) {
            mItem = item;
            mItem.setState(BoxListItem.State.ERROR);

            mThumbView.setImageResource(R.drawable.ic_box_browsesdk_refresh_grey_36dp);
            mThumbView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mMetaDescription.setVisibility(View.VISIBLE);
            mNameView.setText(mContext.getResources().getString(R.string.box_browsesdk_error_retrieving_items));
            mMetaDescription.setText(mContext.getResources().getString(R.string.box_browsesdk_tap_to_retry));
        }

        public void setLoading() {
            mThumbView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mMetaDescription.setVisibility(View.GONE);
            mNameView.setText(mContext.getResources().getString(R.string.boxsdk_Please_wait));
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

        public AppCompatCheckBox getCheckBox() {
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
            if (mListener.getMultiSelectHandler() != null) {
                mListener.getMultiSelectHandler().setEnabled(!mListener.getMultiSelectHandler().isEnabled());
                mListener.getMultiSelectHandler().toggle(mItem.getBoxItem());
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if (mListener.getMultiSelectHandler() != null && mListener.getMultiSelectHandler().isEnabled()) {
                mListener.getMultiSelectHandler().toggle(mItem.getBoxItem());
                onBindBoxItemViewHolder(this);
                return;
            }
            // TODO: Baymax - make sure swipe is disabled while refreshing
//            if (mSwipeRefresh.isRefreshing()) {
//                return;
//            }
            if (mItem == null) {
                return;
            }

            if (mItem.getState() == BoxListItem.State.ERROR) {
                mItem.setState(BoxListItem.State.SUBMITTED);
                mController.execute(mItem.getRequest());
                setLoading();
            }

            if (mListener != null) {
                mListener.getOnItemClickListener().onItemClick(mItem.getBoxItem());

            }
        }

        public void setAccentColor(Resources res, ProgressBar progressBar) {
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

    private class BoxItemClickListener implements View.OnClickListener {

        protected BoxListItem mBoxListItem;

        void setListItem(BoxListItem listItem) {
            mBoxListItem = listItem;
        }

        @Override
        public void onClick(View v) {
            mListener.getOnSecondaryActionListener().onSecondaryAction(mBoxListItem.getBoxItem());
        }
    }

    public interface OnInteractionListener {
        BoxBrowseFragment.MultiSelectHandler getMultiSelectHandler();
        BoxBrowseFragment.OnSecondaryActionListener getOnSecondaryActionListener();
        BoxBrowseFragment.OnItemClickListener getOnItemClickListener();
    }


}