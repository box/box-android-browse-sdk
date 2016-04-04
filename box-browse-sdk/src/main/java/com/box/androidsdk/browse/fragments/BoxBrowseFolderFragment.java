package com.box.androidsdk.browse.fragments;

import android.content.IntentFilter;
import android.os.Bundle;

import com.box.androidsdk.browse.service.BoxResponseIntent;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxIterator;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Use the {@link Builder#build()} to
 * create an instance of this fragment.
 */
public class BoxBrowseFolderFragment extends BoxBrowseFragment {

    private static final String OUT_ITEM = "outItem";
    protected BoxFolder mFolder = null;

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
            mFolder = (BoxFolder) savedInstanceState.getSerializable(OUT_ITEM);
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
        getController().execute(getController().getFolderWithAllItems(mFolder.getId()));
    }

    /**
     * @return the current folder this fragment is meant to display.
     */
    public BoxFolder getFolder() {
        return mFolder;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mFolder);
        super.onSaveInstanceState(outState);
    }

    protected void onFolderFetched(BoxFolder folder) {
        if (folder != null && mFolder.getId().equals(folder.getId())) {
            updateItems(folder.getItemCollection().getEntries());
            mFolder = cloneFolderWithoutItems(folder);

            if (mFolder != null && mFolder.getItemCollection() != null) {
                updateItems(mFolder.getItemCollection().getEntries());
                notifyUpdateListeners();
            }
        }
    }


    /**
     * Convenience method that returns a folder object without its item collection. This is done to ensure
     * a single source of truth for the item collection
     *
     * @param folder
     * @return
     */
    protected BoxFolder cloneFolderWithoutItems(BoxFolder folder) {
        JsonObject jsonObject = folder.toJsonObject();
        JsonValue obj = jsonObject.get(BoxFolder.FIELD_ITEM_COLLECTION);
        if (obj != null && !obj.isNull()) {
            obj.asObject().set(BoxIterator.FIELD_ENTRIES, new JsonArray());
        }
        return new BoxFolder(jsonObject);
    }

    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static class Builder extends BoxBrowseFragment.Builder<BoxBrowseFolderFragment> {

        /**
         * @param folderId id of the folder to browse
         * @param userId   id of the user that the contents will be loaded for
         */
        public Builder(String folderId, String userId) {
            mArgs.putString(ARG_ID, folderId);
            mArgs.putString(ARG_USER_ID, userId);

        }

        /**
         * @param folder  the BoxFolder to Browse
         * @param session the session that the contents will be loaded for
         */
        public Builder(BoxFolder folder, BoxSession session) {
            mArgs.putString(ARG_ID, folder.getId());
            mArgs.putString(ARG_USER_ID, session.getUserId());
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
        protected BoxBrowseFolderFragment getInstance() {
            return new BoxBrowseFolderFragment();
        }
    }

}
