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
import com.box.androidsdk.content.utils.BoxLogUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoxItemAdapter extends RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder> {
    protected final Context mContext;
    protected final BrowseController mController;
    protected final OnInteractionListener mListener;
    protected ArrayList<BoxItem> mItems = new ArrayList<BoxItem>();
    protected HashMap<String, Integer> mItemsPositionMap = new HashMap<String, Integer>();
    protected final Handler mHandler;

    protected int BOX_ITEM_VIEW_TYPE = 0;
    protected static final int REMOVE_LIMIT = 5;

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

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return BOX_ITEM_VIEW_TYPE;
    }


    public synchronized void setItems(ArrayList<BoxItem> items) {
        mItemsPositionMap.clear();
        mItems = items;
        for (int i = 0; i < mItems.size(); ++i) {
            mItemsPositionMap.put(mItems.get(i).getId(), i);
        }
    }

    public boolean contains(BoxItem item) {
        return mItemsPositionMap.containsKey(item.getId());
    }

    public synchronized void removeAll() {
        mItemsPositionMap.clear();
        mItems.clear();
        notifyDataSetChanged();
    }

    public int remove(BoxItem item) {
        if (item == null) {
            return -1;
        }
        return remove(item.getId());
    }

    public synchronized int remove(String id) {
        if (!mItemsPositionMap.containsKey(id)) {
            return -1;
        }
        final Lock lock = new ReentrantLock();
        final int index = mItemsPositionMap.get(id);
        mItems.remove(index);
        mItemsPositionMap.remove(id);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemRemoved(index);
                lock.unlock();
            }
        });
        lock.lock();
        for (int i = index; i < mItems.size(); i++) {
            mItemsPositionMap.put(mItems.get(i).getId(), i);
        }
        return index;
    }

    public synchronized List<String> remove(List<String> ids){
        final Lock lock = new ReentrantLock();
        try {
            lock.lockInterruptibly();
        } catch (Exception e){
            BoxLogUtils.e("lock interrupted", e);
        }
        ArrayList<String> idsRemoved = new ArrayList<String>(ids.size());
        for (String id : ids){
            if (mItemsPositionMap.containsKey(id)) {
                idsRemoved.add(id);
            }
        }
        final ArrayList<Integer> indexesRemoved = new ArrayList<Integer>(idsRemoved.size());

        for (String id : idsRemoved){
            final int index = mItemsPositionMap.get(id);
            indexesRemoved.add(index);
            mItemsPositionMap.remove(id);

        }

        Collections.sort(indexesRemoved);
        for (int i= indexesRemoved.size()-1; i >= 0; i--){
            Integer index = indexesRemoved.get(i);
            mItems.remove(index.intValue());
        }
        if (indexesRemoved.size() > 0) {
            int smallestIndex = indexesRemoved.get(0);
            for (int i = smallestIndex; i < mItems.size(); i++) {
                mItemsPositionMap.put(mItems.get(i).getId(), i);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (indexesRemoved.size() < 5){
                        for (Integer index : indexesRemoved){
                            notifyItemRemoved(index);
                        }
                    } else {
                        notifyDataSetChanged();
                    }
                }
            });

        }
        return idsRemoved;
    }


    /**
     * Does the appropriate add and removes to display only provided items.
     * @param items
     */
    public synchronized void updateTo(final ArrayList<BoxItem> items){
        final Lock lock = new ReentrantLock();
        try {
            lock.lockInterruptibly();
        } catch (Exception e){
            BoxLogUtils.e("lock interrupted", e);
        }
        if (items.size() == mItems.size() && items.size() > 0){
            boolean isSame = true;
            // check to see if this is a duplicate call if so ignore it.
            for(int i=0; i < mItems.size(); i++){
                if (items.get(i) != mItems.get(i)){
                    if (!items.get(i).equals(mItems.get(i))){
                        isSame = false;
                        break;
                    }
                }
            }
            if (isSame){
                return;
            }
        }

        final HashMap<String, Integer> oldPositionMap = mItemsPositionMap;
        final HashMap<String, Integer> newPositionMap = new HashMap<String, Integer>(items.size());

        for (int i=0; i < items.size(); i++) {
            newPositionMap.put(items.get(i).getId(), i);
        }
        // if we are updating to a smaller size list we must delete old entries first.

        if (oldPositionMap.size() > newPositionMap.size() ) {


            Iterator<Map.Entry<String,Integer>> iterator = oldPositionMap.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,Integer> entry = iterator.next();
                if (newPositionMap.containsKey(entry.getKey())){
                    iterator.remove();
                }
            }

            // if the user is removing over the remove limit it's not worth removing them individually.
            if (oldPositionMap.size() > REMOVE_LIMIT){
                mItems.clear();
                mItems.addAll(items);
                mItemsPositionMap = newPositionMap;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
                return;
            }

            // NOTE:
            // We need to notify item removed in descending order of index
            // Otherwise once we remove the 1st item, index of all others would change
            // and we will end up removing incorrect items
            final ArrayList<Integer> removedIndexes = new ArrayList<Integer>(oldPositionMap.size());
            removedIndexes.addAll(oldPositionMap.values());
            Collections.sort(removedIndexes);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                     for (int i = removedIndexes.size() - 1; i >= 0; i--) {
                        mItems.remove(removedIndexes.get(i)); //Remove individual items
                        notifyItemRemoved(removedIndexes.get(i));
                    }
                    mItems.clear();
                    mItems.addAll(items); //Update everything since we have new info.
                    mItemsPositionMap = newPositionMap;
                    lock.unlock();

                }
            });

        } else {
            mItems.clear();
            mItems.addAll(items);
            mItemsPositionMap = newPositionMap;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(0, mItems.size());
                    lock.unlock();
                }
            });

        }

    }

    public void addAll(ArrayList<BoxItem> items) {
        final Lock lock = new ReentrantLock();
        try {
            lock.lockInterruptibly();
        } catch (Exception e){
            BoxLogUtils.e("lock interrupted", e);
        }
        final int startingSize = mItems.size();
        for (BoxItem item : items) {
            add(item);
        }
        final int endingSize = mItems.size();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemRangeInserted(startingSize - 1, endingSize -1);
                lock.unlock();
            }
        });
    }

    /**
     * This method will add an item to the end of the list, but will not notify of the change.
     * @param item
     */
    protected synchronized void add(BoxItem item) {
        mItems.add(item);
        mItemsPositionMap.put(item.getId(), mItems.size() - 1);
    }

    public int update(BoxItem item) {
        if (item == null || !mItemsPositionMap.containsKey(item.getId())) {
            return -1;
        }
        final Lock lock = new ReentrantLock();
        try {
            lock.lockInterruptibly();
        } catch (Exception e){
            BoxLogUtils.e("lock interrupted", e);
        }

        final int index = mItemsPositionMap.get(item.getId());
        mItems.set(index, item);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(index);
                lock.unlock();
            }
        });
        return index;
    }

    public int indexOf(String id) {
        if (!mItemsPositionMap.containsKey(id)) {
            return -1;
        }

        return mItemsPositionMap.get(id);
    }

    public ArrayList<BoxItem> getItems() {
        return mItems;
    }

    @Override
    public long getItemId(int position) {
        try {
            return Long.parseLong(mItems.get(position).getId());
        } catch (NumberFormatException e){
            return mItems.get(position).getId().hashCode();
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