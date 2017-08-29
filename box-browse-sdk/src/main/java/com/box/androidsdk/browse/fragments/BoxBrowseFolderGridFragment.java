package com.box.androidsdk.browse.fragments;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxItemAdapter;
import com.box.androidsdk.browse.adapters.BoxMediaItemAdapter;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIterator;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.io.Serializable;

/**
 * Fragment to browse a box folder
 *
 * Use the {@link Builder#build()} to
 * create an instance of this fragment.
 */
public class BoxBrowseFolderGridFragment extends BoxBrowseFragment {

    public static final String ARG_FOLDER = "BoxBrowseFolderFragment.Folder";
    protected BoxFolder mFolder = null;


    protected void initRecyclerView(RecyclerView view){
        int numColumns = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ? 5 : 3;
        view.setLayoutManager(new GridLayoutManager(getActivity(),numColumns));
        view.addItemDecoration(new SpacesItemDecoration(8));
        if (view.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) view.getItemAnimator()).setSupportsChangeAnimations(false);
        }
    }

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {

        private int halfSpace;

        public SpacesItemDecoration(int space) {
            this.halfSpace = space / 2;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

            if (parent.getPaddingLeft() != halfSpace) {
                parent.setPadding(halfSpace, halfSpace, halfSpace, halfSpace);
                parent.setClipToPadding(false);
            }

            outRect.top = halfSpace;
            outRect.bottom = halfSpace;
            outRect.left = halfSpace;
            outRect.right = halfSpace;
        }
    }

    protected BoxItemAdapter createAdapter() {
        return new BoxMediaItemAdapter(getActivity(), getController(), this);
    }

    protected int getLayout() {
        return R.layout.box_browsesdk_fragment_browse_media;
    }



    @Override
    protected IntentFilter getIntentFilter() {
        IntentFilter filter = super.getIntentFilter();
        filter.addAction(BoxRequestsFolder.GetFolderWithAllItems.class.getName());
        return filter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFolder = (BoxFolder) savedInstanceState.getSerializable(ARG_FOLDER);
        } else if (getArguments() != null) {
            String folderId = getArguments().getString(ARG_ID);
            String folderName = getArguments().getString(ARG_NAME);
            if (mFolder == null && !SdkUtils.isBlank(folderId)) {
                mFolder = BoxFolder.createFromIdAndName(folderId, folderName);
            }
        }
    }

    @Override
    protected void handleResponse(BoxResponseIntent intent) {
        super.handleResponse(intent);
        if (!intent.isSuccess()) {
            checkConnectivity();
            return;
        }
        if (intent.getAction().equals(BoxRequestsFolder.GetFolderWithAllItems.class.getName())) {
            onFolderFetched((BoxFolder) intent.getResult());
            if (mSwipeRefresh != null) {
                mSwipeRefresh.setRefreshing(false);
            }
        }
    }

    @Override
    protected void loadItems() {
        mProgress.setVisibility(View.VISIBLE);
        getController().execute(getController().getFolderWithAllItems(mFolder.getId()));
    }

    /**
     * Gets the BoxFolder being displayed by this fragment
     *
     * @return the current folder this fragment is meant to display.
     */
    public BoxFolder getFolder() {
        return mFolder;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_FOLDER, mFolder);
        super.onSaveInstanceState(outState);
    }

    /**
     * On folder fetched.
     *
     * @param folder that has been fetched
     */
    protected void onFolderFetched(BoxFolder folder) {
        if (folder != null && mFolder.getId().equals(folder.getId())) {
            BoxIteratorItems items = folder.getItemCollection();
            if (items != null && items.getEntries() != null && items.fullSize() != null && (items.size() > 0 || items.fullSize() == 0)) {
                updateItems(folder.getItemCollection().getEntries());
            }
            mFolder = createFolderWithoutItems(folder);
            notifyUpdateListeners();
        }
    }

    @Override
    public BoxItemFilter getItemFilter() {
        return super.getItemFilter();
    }

    /**
     * Convenience method that returns a folder object without its item collection. This is done to ensure
     * a single source of truth for the item collection
     *
     * @param folder the folder which may contain items
     * @return box folder without any items
     */
    protected BoxFolder createFolderWithoutItems(BoxFolder folder) {
        JsonObject jsonObject = new JsonObject();
        for (String key: folder.getPropertiesKeySet()){
            if (!key.equals(BoxFolder.FIELD_ITEM_COLLECTION)){
                jsonObject.add(key, folder.getPropertyValue(key));
            } else {
                JsonObject itemCollection = new JsonObject();
                BoxIteratorItems iteratorItems = folder.getItemCollection();
                for (String collectionKey : iteratorItems.getPropertiesKeySet()){
                    if (!collectionKey.equals(BoxIterator.FIELD_ENTRIES)) {
                        itemCollection.add(collectionKey, iteratorItems.getPropertyValue(collectionKey));
                    } else {
                        itemCollection.add(collectionKey, new JsonArray());
                    }
                }
                jsonObject.add(key, itemCollection);
            }
        }
        return new BoxFolder(jsonObject);
    }

    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static class Builder extends BoxBrowseFragment.Builder<BoxBrowseFolderGridFragment> {

        /**
         * Instantiates a new Builder.
         *
         * @param folderId id of the folder to browse
         * @param userId   id of the user that the contents will be loaded for
         */
        public Builder(String folderId, String userId) {
            mArgs.putString(ARG_ID, folderId);
            mArgs.putString(ARG_USER_ID, userId);
            setBoxItemFilter(new MediaItemFilter());
        }

        /**
         * Instantiates a new Builder.
         *
         * @param folder  the BoxFolder to Browse
         * @param session the session that the contents will be loaded for
         */
        public Builder(BoxFolder folder, BoxSession session) {
            mArgs.putString(ARG_ID, folder.getId());
            mArgs.putString(ARG_NAME, folder.getName());
            mArgs.putString(ARG_USER_ID, session.getUserId());
            setBoxItemFilter(new MediaItemFilter());
        }

        /**
         * Set the name of the folder that will be shown as title in the toolbar
         *
         * @param folderName
         */
        public void setFolderName(String folderName) {
            mArgs.putString(ARG_NAME, folderName);
        }

        @Override
        protected BoxBrowseFolderGridFragment getInstance() {
            return new BoxBrowseFolderGridFragment();
        }

        public static class MediaItemFilter implements BoxItemFilter, Serializable{

            @Override
            public boolean accept(BoxItem item) {
                return ThumbnailManager.isThumbnailAvailable(item) || ThumbnailManager.isVideo(item);
            }

            @Override
            public boolean isEnabled(BoxItem item) {
                return true;
            }
        }
    }

}
