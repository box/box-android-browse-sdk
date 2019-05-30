package com.box.androidsdk.browse.adapters;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;
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
import com.box.androidsdk.content.utils.SdkUtils;

import java.lang.ref.WeakReference;
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

/**
 * Adapter for BoxItems used in the RecyclerView of a BrowseFragment
 */
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
    WeakReference<RecyclerView> mRecyclerViewRef;

    static final int DELAY = 50;


    /**
     * Instantiates a new Box item adapter.
     *
     * @param context    the context
     * @param controller BrowseController instance
     * @param listener   OnInteractionListener to net notified for any interactions with the items
     */
    public BoxItemAdapter(Context context, BrowseController controller, OnInteractionListener listener) {
        mContext = context;
        mController = controller;
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper());
    }



    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerViewRef = new WeakReference<RecyclerView>(recyclerView);
        super.onAttachedToRecyclerView(recyclerView);
    }

    /**
     * A check to see if the recyclerview is busy and operations altering it should not be made.
     *
     * @return true if the recyclerview is currently computing its layout, false otherwise.
     */
    protected boolean isRecyclerViewComputing(){
        if (mRecyclerViewRef == null && mRecyclerViewRef.get() != null){
            boolean isComputing = mRecyclerViewRef.get().isComputingLayout();
            return isComputing;
        }
        return false;
    }

    /**
     * Is code running on ui thread
     *
     * @return true if this method is being run on the ui thread, false otherwise.
     */
    protected boolean isOnUiThread(){
        return mHandler.getLooper().getThread().equals(Thread.currentThread());
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

    /**
     * Gets a hash map with item ids as keys and their position as values
     *
     * @param items the items
     * @return the hash map with item ids as keys and their position as values
     */
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

    /**
     * Clear all items from the adapter. This method is always run on ui
     * thread so adapter may not reflect changes immediately.
     */
    public void removeAll() {
        if (isRecyclerViewComputing() || ! isOnUiThread()){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeAll();
                }
            }, DELAY);
            return;
        }
        Lock lock = mLock.writeLock();
        lock.lock();
        try {
            mItems.clear();
            notifyDataSetChanged();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the ids from this folder if applicable. This method is always run on ui
     * thread so adapter may not reflect changes immediately.
     *
     * @param ids list of ids to remove
     */
    public void remove(final List<String> ids){
        if (isRecyclerViewComputing()  || ! isOnUiThread()){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    remove(ids);
                }
            }, DELAY);
            return;
        }
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

    /**
     * Does the appropriate add and removes to display only provided items. This method is always run on ui
     * thread so adapter may not reflect changes immediately.
     *
     * @param items new list of items adapter should reflect.
     */
    public void updateTo(final ArrayList<BoxItem> items){
        if (isRecyclerViewComputing() ||  ! isOnUiThread()){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateTo(items);
                }
            }, DELAY);
            return;
        }
        final Lock writeLock = mLock.writeLock();
        writeLock.lock();
       try{
            if (mItems.size() == 0){
                // if going from completely empty to having something do not bother animating.
                mItems.clear();
                mItems.addAll(items);
                notifyDataSetChanged();
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
                    mItems.clear();
                    mItems.addAll(items);
                    for (Integer insertIndex : newItemPositions) {
                        notifyItemInserted(insertIndex);
                    }
                    notifyItemRangeChanged(0, mItems.size());
                } else if (oldPositionMap.size() > 0 && oldPositionMap.size() <= REMOVE_LIMIT){
                    final ArrayList<Integer> indexesRemoved = new ArrayList<Integer>(oldPositionMap.size());
                    indexesRemoved.addAll(oldPositionMap.values());
                    Collections.sort(indexesRemoved);
                    for (int i = indexesRemoved.size() - 1; i >= 0; i--) {
                        notifyItemRemoved(indexesRemoved.get(i));
                    }
                    mItems.clear();
                    mItems.addAll(items);
                    notifyItemRangeChanged(0, mItems.size());
              } else {
                    // for everything else, mixed operations or oeprations beyond limits
                    mItems.clear();
                    mItems.addAll(items);
                    notifyDataSetChanged();
                }
            }
       } finally {
            writeLock.unlock();
       }


    }

    /**
     * Add items to the end of the adapter. This method is always run on ui
     * thread so adapter may not reflect changes immediately.
     *
     * @param items to append to this adapter.
     */
    public void add(final List<BoxItem> items) {
        if (items.size() == 0){
            return;
        }

        if (isRecyclerViewComputing() || ! isOnUiThread()){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    add(items);
                }
            }, DELAY);
            return;
        }

        final Lock lock = mLock.writeLock();
        lock.lock();
        mItems.addAll(items);
        try {
            notifyDataSetChanged();
        }finally{
            lock.unlock();
        }
    }

    /**
     * Update an item inside of the adapter if applicable. This method is always run on ui
     * thread so adapter may not reflect changes immediately.
     *
     * @param item item to update.
     */
    public void update(final BoxItem item) {

        if (isRecyclerViewComputing() || ! isOnUiThread()){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    update(item);
                }
            }, DELAY);
            return;
        }
        final Lock lock = mLock.writeLock();
        lock.lock();
        try{
            for (int i=0; i < mItems.size(); i++){
                if(mItems.get(i).getId().equals(item.getId())){
                    final int index = i;
                    mItems.set(index, item);
                    notifyItemChanged(index);
                    return;
                }
            }

            return;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the index of an item in the adapter. It may return stale data in case an
     * update method is pending.
     *
     * @param id the box item id to check for.
     * @return the index of the box item id.
     */
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

    /**
     * It may return stale data in case an
     * update method is pending.
     *
     * @return A new list containing the items shown by this adapter.
     */
    public ArrayList<BoxItem> getItems() {
        mLock.readLock().lock();
        try {
            return (ArrayList<BoxItem>) mItems.clone();
        } finally{
            mLock.readLock().unlock();
        }
    }

    /**
     * It may return stale data in case an
     * update method is pending.
     * @param position an index position
     * @return a box item id for the item at that position.
     */
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


    /**
     * View Holder for the BoxItemAdapater
     */
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

        /**
         * Instantiates a new Box item view holder.
         *
         * @param itemView the item view
         */
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

        /**
         * Bind item.
         *
         * @param item the item
         */
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
         * @param holder     the BoxItemHolder
         * @param itemToBind the item to bind
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
                        SdkUtils.getLocalizedFileSize(mContext, itemToBind.getSize()) :
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
         * Gets check box from the view represented by this view holder
         *
         * @return the check box
         */
        public AppCompatCheckBox getCheckBox() {
            return mItemCheckBox;
        }

        /**
         * Gets secondary action view from the view represented by this view holder
         *
         * @return the secondary action
         */
        public ImageButton getSecondaryAction() {
            return mSecondaryAction;
        }


        /**
         * Gets the box item which is displayed by this viewholder
         *
         * @return the item
         */
        public BoxItem getItem() {
            return mItem;
        }

        /**
         * Gets progress bar.
         *
         * @return the progress bar
         */
        public ProgressBar getProgressBar() {
            return mProgressBar;
        }

        /**
         * Gets meta description view
         *
         * @return the meta description
         */
        public TextView getMetaDescription() {
            return mMetaDescription;
        }

        /**
         * Gets name view.
         *
         * @return the name view
         */
        public TextView getNameView() {
            return mNameView;
        }

        /**
         * Gets thumbnail view.
         *
         * @return the thumb view
         */
        public ImageView getThumbView() {
            return mThumbView;
        }

        /**
         * Gets the root view represented by this view holder
         *
         * @return the view
         */
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

        /**
         * Sets list item.
         *
         * @param item the item
         */
        void setListItem(BoxItem item) {
            mItem = item;
        }

        @Override
        public void onClick(View v) {
            mListener.getOnSecondaryActionListener().onSecondaryAction(mItem);
        }
    }

    /**
     * The interface On interaction listener.
     */
    public interface OnInteractionListener {
        /**
         * returns the MultiSelectHandler set on BrowseFragment.
         *
         * @return the multi select handler
         */
        BoxBrowseFragment.MultiSelectHandler getMultiSelectHandler();

        /**
         * Gets on secondary action listener set on BrowseFragment.
         *
         * @return the on secondary action listener
         */
        BoxBrowseFragment.OnSecondaryActionListener getOnSecondaryActionListener();

        /**
         * Gets on item click listener set on BrowseFragment.
         *
         * @return the on item click listener
         */
        BoxBrowseFragment.OnItemClickListener getOnItemClickListener();

        /**
         * Gets item filter
         *
         * @return the item filter
         */
        BoxItemFilter getItemFilter();

    }


}