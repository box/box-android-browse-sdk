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
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Adapter for Media items used in the RecyclerView of a BrowseFragment
 */
public class BoxMediaItemAdapter extends BoxItemAdapter {

    /**
     * Instantiates a new Box item adapter.
     *
     * @param context    the context
     * @param controller BrowseController instance
     * @param listener   OnInteractionListener to net notified for any interactions with the items
     */
    public BoxMediaItemAdapter(Context context, BrowseController controller, OnInteractionListener listener) {
        super(context, controller, listener);
    }



    @Override
    public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_media_item, viewGroup, false);
        return new BoxMediaItemViewHolder(view);
    }

    /**
     * View Holder for the BoxItemAdapater
     */
    public class BoxMediaItemViewHolder extends BoxItemAdapter.BoxItemViewHolder {
        /**
         * Instantiates a new Box item view holder.
         *
         * @param itemView the item view
         */
        public BoxMediaItemViewHolder(View itemView) {
            super(itemView);
        }


        /**
         * Called when a {@link BoxItem} is bound to a ViewHolder. Customizations of UI elements
         * should be done by overriding this method. If extending from a {@link BoxBrowseActivity}
         * a custom BoxBrowseFolder fragment can be returned in
         * {@link BoxBrowseActivity#createBrowseFolderFragment(BoxItem, BoxSession)}
         *
         * @param holder     the BoxItemHolder
         * @param itemToBind the item to bind
         */
        @Override
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
                mController.getThumbnailManager().loadMediaThumbnail(itemToBind, holder.getThumbView());
            }
            holder.getProgressBar().setVisibility(View.GONE);
            holder.getThumbView().setVisibility(View.VISIBLE);

            boolean isEnabled = mListener.getItemFilter() == null || mListener.getItemFilter().isEnabled(itemToBind);
            holder.getView().setEnabled(isEnabled);
            if (isEnabled) {
                holder.getThumbView().setAlpha(1f);
            } else {
                holder.getThumbView().setAlpha(.3f);
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

            //show video icon
            if (ThumbnailManager.isVideo(itemToBind)){
                holder.getNameView().setVisibility(View.VISIBLE);
            } else {
                holder.getNameView().setVisibility(View.GONE);
            }

        }

        /**
         * Gets meta description view
         *
         * @return the meta description
         */
        @Deprecated
        public TextView getMetaDescription() {
            return mMetaDescription;
        }

        /**
         * Gets name view.
         *
         * @return the name view
         */
        @Deprecated
        public TextView getNameView() {
            return mNameView;
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



}