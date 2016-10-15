package com.box.androidsdk.browse.adapters;


import android.content.Context;
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
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BoxItemAdapter extends RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder> {
    protected final Context mContext;
    protected final BrowseController mController;
    protected final OnInteractionListener mListener;
    protected final ArrayList<BoxItem> mItems = new ArrayList<BoxItem>();
    protected final Handler mHandler;

    protected int BOX_ITEM_VIEW_TYPE = 0;
    protected static final int REMOVE_LIMIT = 5;
    protected static final int INSERT_LIMIT = 10;
    protected ReadWriteLock mLock = new ReentrantReadWriteLock();

    public BoxItemAdapter(Context context, BrowseController controller, OnInteractionListener listener) {
        mContext = context;
        mController = controller;
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
        return new BoxItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BoxItemViewHolder boxItemHolder, int i) {
        BoxItem item = mItems.get(i);
        boxItemHolder.bindItem(item);
    }

    protected HashMap<String, Integer> getPositionMap(final List<BoxItem> items){
        HashMap<String, Integer> map = new HashMap<String, Integer>(items.size());
        for (int i= 0;i < items.size(); i++){
            map.put(items.get(i).getId(), i);
        }
        return map;
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return BOX_ITEM_VIEW_TYPE;
    }

    public void removeAll() {
        Lock lock = mLock.writeLock();
        lock.lock();
        try {
            mItems.clear();
        } finally {
            lock.unlock();
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLock.readLock().lock();
                notifyDataSetChanged();
                mLock.readLock().unlock();
            }
        });
    }


    /**
     * Removes the ids from this folder if applicable.
     * @param ids
     */
    public void remove(final List<String> ids){
        mLock.readLock().lock();
        try {
            // check to see if any of the ids are applicable to the data set.
            HashMap<String, Integer> itemsPositionMap = getPositionMap(mItems);
            boolean foundInItems = false;
            for (String id: ids){
                if (itemsPositionMap.containsKey(id)){
                    foundInItems = true;
                    break;
                }
            }
            if (!foundInItems){
                // none of the ids are applicable for this data set no need to proceed.
                return;
            }

        } finally{
            mLock.readLock().unlock();
        }
        final Lock writeLock = mLock.writeLock();
        writeLock.lock();
            mHandler.post(new Runnable() {
                @Override
                public void run() {


                    HashSet<String> idsRemoved = new HashSet<String>(ids.size());
                    try {
                        final ArrayList<Integer> indexesRemoved = new ArrayList<Integer>(ids.size());
                        HashMap<String, Integer> mItemsPositionMap = getPositionMap(mItems);
                        for (String id : ids) {
                            Integer index = mItemsPositionMap.get(id);
                            if (index != null) {
                                idsRemoved.add(id);
                                indexesRemoved.add(index);
                            }
                        }
                        // because we are using an array list we don't want to do multiple removes (as this needs to create a new array and does array copies).
                        final ArrayList<BoxItem> listWithoutRemovedIds = new ArrayList<BoxItem>(mItems.size() - idsRemoved.size());
                        for (BoxItem item : mItems) {
                            if (!idsRemoved.contains(item.getId())) {
                                listWithoutRemovedIds.add(item);
                            }
                        }

                        boolean removedItems = false;
                        if (indexesRemoved.size() <= REMOVE_LIMIT) {
                            Collections.sort(indexesRemoved);
                            for (int i=indexesRemoved.size() -1; i >= 0; i--){
                                notifyItemRemoved(indexesRemoved.get(i));
                            }
                            removedItems = true;
                        }
                        // we need to alter the list after the remove.

                        mItems.clear();
                        mItems.addAll(listWithoutRemovedIds);

                        if (removedItems && mItems.size() > 0) {
                            notifyItemRangeChanged(0, mItems.size());
                        } else {
                            notifyDataSetChanged();
                        }
                    } finally {
                        writeLock.unlock();
                    }
                }
            });
    }

    /**
     * Does the appropriate add and removes to display only provided items.
     * @param items
     */
    public void updateTo(final ArrayList<BoxItem> items){
        final Lock writeLock = mLock.writeLock();
        writeLock.lock();
        boolean shouldUnlockInFinally = true;

        try {
            if (mItems.size() == 0){
                // if going from completely empty to having something do not bother animating.
                mItems.clear();
                mItems.addAll(items);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLock.readLock().lock();
                        notifyDataSetChanged();
                        mLock.readLock().unlock();
                    }
                });
                return;
            }

            final HashMap<String, Integer> oldPositionMap = getPositionMap(mItems);
            final ArrayList<Integer> newItemPositions = new ArrayList<Integer>();
            boolean needsContentUpdate = false;
            for (int i=0; i < items.size(); i++){
                Integer index = oldPositionMap.remove(items.get(i).getId());
                if (index == null){
                    needsContentUpdate = true;
                    newItemPositions.add(i);
                } else if (!needsContentUpdate){

                    if (index.equals(i)){
                        if (!(mItems.get(i) == items.get(i))) {
                            // check to see if the contents have changed for items with the same index.
                            needsContentUpdate = !mItems.get(i).equals(items.get(i));
                        }
                    } else {
                        needsContentUpdate = true;
                    }
                }
            }
            needsContentUpdate |= oldPositionMap.size() > 0 || newItemPositions.size() > 0 || items.size() == 0;
            if (!needsContentUpdate ){
                return;
            } else {
                if (oldPositionMap.size() == 0 && newItemPositions.size() > 0 && newItemPositions.size() <= INSERT_LIMIT){
                    shouldUnlockInFinally = false;
                    mItems.clear();
                    mItems.addAll(items);
                    // for inserts less than max.
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // in order to show animations of new items
                                for (Integer insertIndex : newItemPositions) {
                                    notifyItemInserted(insertIndex);
                                }
                                notifyItemRangeChanged(0, mItems.size());
                            } finally {
                                writeLock.unlock();
                            }
                        }
                    });

                } else if (oldPositionMap.size() > 0 && oldPositionMap.size() <= REMOVE_LIMIT){
                    final ArrayList<Integer> indexesRemoved = new ArrayList<Integer>(oldPositionMap.size());
                    indexesRemoved.addAll(oldPositionMap.values());
                    Collections.sort(indexesRemoved);
                    shouldUnlockInFinally = false;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int i = indexesRemoved.size() - 1; i >= 0; i--) {
                                    notifyItemRemoved(indexesRemoved.get(i));
                                }
                                mItems.clear();
                                mItems.addAll(items);
                                notifyItemRangeChanged(0, mItems.size());
                            } finally {
                                writeLock.unlock();
                            }
                        }
                    });


                } else {
                    // for everything else, mixed operations or oeprations beyond limits
                    mItems.clear();
                    mItems.addAll(items);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mLock.readLock().lock();
                            notifyDataSetChanged();
                            mLock.readLock().unlock();
                        }
                    });
                }
            }
        } finally {
            if (shouldUnlockInFinally){
                writeLock.unlock();
            }
        }
    }

    public void add(List<BoxItem> items) {
        if (items.size() == 0){
            return;
        }

        final Lock lock = mLock.writeLock();
        lock.lock();
        final int startingSize = mItems.size();
        mItems.addAll(items);
        final int endingSize = mItems.size();

        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        notifyItemRangeInserted(startingSize - 1, endingSize - 1);
                    }finally{
                        lock.unlock();
                    }
                }
            });

    }

    public int update(BoxItem item) {
        final Lock lock = mLock.writeLock();
        lock.lock();
        try{
            for (int i=0; i < mItems.size(); i++){
                if(mItems.get(i).getId().equals(item.getId())){
                    final int index = i;
                    mItems.set(index, item);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mLock.readLock().lock();
                            notifyItemChanged(index);
                            mLock.readLock().unlock();
                        }
                    });
                    return index;
                }
            }

            return -1;
        } finally {
            lock.unlock();
        }
    }

    public int indexOf(String id) {
        mLock.readLock().lock();
        try {
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getId().equals((id))) {
                    return i;
                }
            }
        } finally {
            mLock.readLock().unlock();
        }
        return -1;
    }

    public ArrayList<BoxItem> getItems() {
        mLock.readLock().lock();
        try {
            return (ArrayList<BoxItem>) mItems.clone();
        } finally{
            mLock.readLock().unlock();
        }
    }

    @Override
    public long getItemId(int position) {
        mLock.readLock().lock();
        try {
            return Long.parseLong(mItems.get(position).getId());
        } catch (NumberFormatException e){
            return mItems.get(position).getId().hashCode();
        } finally {
            mLock.readLock().unlock();
        }
    }


    public class BoxItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        BoxItem mItem;

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
            if (mSecondaryAction != null) {
                mSecondaryAction.setOnClickListener(mSecondaryClickListener);
            }
        }

        public void bindItem(BoxItem item) {
            onBindBoxItemViewHolder(this, item);
            mItem = item;
            mSecondaryClickListener.setListItem(mItem);
        }


        private static final String DESCRIPTION_TEMPLATE = "%s  • %s";

        /**
         * Called when a {@link BoxItem} is bound to a ViewHolder. Customizations of UI elements
         * should be done by overriding this method. If extending from a {@link BoxBrowseActivity}
         * a custom BoxBrowseFolder fragment can be returned in
         * {@link BoxBrowseActivity#createBrowseFolderFragment(BoxItem, BoxSession)}
         *
         * @param holder the BoxItemHolder
         * @param itemToBind
         */
        protected void onBindBoxItemViewHolder(BoxItemViewHolder holder, BoxItem itemToBind) {
            if (itemToBind == null) {
                return;
            }

            final BoxItem prevItem = holder.getItem();
            boolean isSame = prevItem != null && prevItem.getId() != null &&
                    prevItem.getId().equals(itemToBind.getId()) &&
                    prevItem.getModifiedAt() != null &&
                    prevItem.getModifiedAt().equals(itemToBind.getModifiedAt()) &&
                    prevItem.getSize() != null &&
                    prevItem.getSize().equals(itemToBind.getSize());

            if (isSame) {
                // Additional checks for folders
                if(prevItem instanceof BoxFolder) {
                    BoxFolder prevFolder = (BoxFolder) prevItem;
                    BoxFolder folderToBind = (BoxFolder) itemToBind;

                    isSame = prevFolder.getHasCollaborations() == folderToBind.getHasCollaborations();
                }
            }

            if (!isSame) {
                holder.getNameView().setText(itemToBind.getName());
                String modifiedAt = itemToBind.getModifiedAt() != null ?
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(itemToBind.getModifiedAt()) :
                        "";
                String size = itemToBind.getSize() != null ?
                        localFileSizeToDisplay(itemToBind.getSize()) :
                        "";
                String description = String.format(Locale.ENGLISH, DESCRIPTION_TEMPLATE, modifiedAt, size);
                holder.getMetaDescription().setText(description);
                mController.getThumbnailManager().loadThumbnail(itemToBind, holder.getThumbView());
            }
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getMetaDescription().setVisibility(View.VISIBLE);
            holder.getThumbView().setVisibility(View.VISIBLE);

            boolean isEnabled = mListener.getItemFilter() == null || mListener.getItemFilter().isEnabled(itemToBind);
            holder.getView().setEnabled(isEnabled);
            if (isEnabled) {
                holder.getThumbView().setAlpha(1f);
                holder.getNameView().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_primary_text));
                holder.getMetaDescription().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_hint));
            } else {
                holder.getThumbView().setAlpha(.3f);
                holder.getNameView().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_hint));
                holder.getMetaDescription().setTextColor(mContext.getResources().getColor(R.color.box_browsesdk_disabled_hint));
            }

            if (isEnabled && mListener.getOnSecondaryActionListener() != null) {
                holder.getSecondaryAction().setVisibility(View.VISIBLE);
            } else {
                holder.getSecondaryAction().setVisibility(View.GONE);
            }

            if (mListener.getMultiSelectHandler() != null && mListener.getMultiSelectHandler().isEnabled()) {
                holder.getSecondaryAction().setVisibility(View.GONE);
                holder.getCheckBox().setVisibility(View.VISIBLE);
                holder.getCheckBox().setEnabled(isEnabled && mListener.getMultiSelectHandler().isSelectable(itemToBind));
                holder.getCheckBox().setChecked(isEnabled && mListener.getMultiSelectHandler().isItemSelected(itemToBind));
            } else {
                holder.getCheckBox().setVisibility(View.GONE);
            }

            if (mListener.getItemFilter() != null){
                if (mListener.getItemFilter().isEnabled(itemToBind)){
                    getView().setAlpha(1f);
                } else {
                    getView().setAlpha(.5f);
                }
            }

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
                textSize = String.format(Locale.getDefault(), "%4.1f KB", size);
            } else if ((numSize >= constMB) && (numSize < constGB)) {
                size = numSize / floatMB;
                textSize = String.format(Locale.getDefault(), "%4.1f MB", size);
            } else if (numSize >= constGB) {
                size = numSize / floatGB;
                textSize = String.format(Locale.getDefault(), "%4.1f GB", size);
            }
            return textSize;
        }

        public AppCompatCheckBox getCheckBox() {
            return mItemCheckBox;
        }

        public ImageButton getSecondaryAction() {
            return mSecondaryAction;
        }


        public BoxItem getItem() {
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
                if (mListener.getMultiSelectHandler().isEnabled()) {
                    mListener.getMultiSelectHandler().deselectAll();
                    mListener.getMultiSelectHandler().setEnabled(false);
                } else {
                    mListener.getMultiSelectHandler().setEnabled(true);
                    mListener.getMultiSelectHandler().toggle(mItem);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if (mListener.getMultiSelectHandler() != null && mListener.getMultiSelectHandler().isEnabled()) {
                mListener.getMultiSelectHandler().toggle(mItem);
                onBindBoxItemViewHolder(this, mItem);
                return;
            }
            if (mItem == null) {
                return;
            }

            if (mListener != null) {
                mListener.getOnItemClickListener().onItemClick(mItem);

            }
        }
    }

    private class BoxItemClickListener implements View.OnClickListener {

        protected BoxItem mItem;

        void setListItem(BoxItem item) {
            mItem = item;
        }

        @Override
        public void onClick(View v) {
            mListener.getOnSecondaryActionListener().onSecondaryAction(mItem);
        }
    }

    public interface OnInteractionListener {
        BoxBrowseFragment.MultiSelectHandler getMultiSelectHandler();
        BoxBrowseFragment.OnSecondaryActionListener getOnSecondaryActionListener();
        BoxBrowseFragment.OnItemClickListener getOnItemClickListener();
        BoxItemFilter getItemFilter();

    }


}