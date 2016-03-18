package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.browse.service.CompletionListener;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.Serializable;

/**
 * Use the {@link Builder#buildInstance()} to
 * create an instance of this fragment.
 */
public class BoxBrowseFolderFragment extends BoxBrowseFragment {

    protected BoxFolder mFolder = null;
    protected BoxApiFolder mFolderApi = null;
    private static final String OUT_ITEM = "outItem";





    /**
     * Builder for constructing an instance of BoxBrowseFolderFragment
     */
    public static class Builder {
        Bundle mArgs = new Bundle();


        /**
         * @param folderId id of the folder to browse
         * @param userId id of the user that the contents will be loaded for
         */
        public Builder(String folderId, String userId) {
            mArgs.putString(ARG_ID, folderId);
            mArgs.putString(ARG_USER_ID, userId);
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);

        }

        /**
         *
         * @param folder the BoxFolder to Browse
         * @param session the session that the contents will be loaded for
         */
        public Builder(BoxFolder folder, BoxSession session) {
            mArgs.putString(ARG_ID, folder.getId());
            mArgs.putString(ARG_USER_ID, session.getUserId());
            mArgs.putInt(ARG_LIMIT, DEFAULT_LIMIT);
        }

        /**
         * Set the name of the folder that will be shown as title in the toolbar
         * @param folderName
         */
        public void setFolderName(String folderName) {
            mArgs.putString(ARG_NAME, folderName);
        }

        /**
         * Set the number of items that the results will be limited to when retrieving folder items
         * @param limit
         */
        public void setLimit(int limit) {
            mArgs.putInt(ARG_LIMIT, limit);
        }

        /**
         *  Set the BoxItemFilter for filtering the items being displayed
         * @param filter
         * @param <E>
         */
        public <E extends Serializable & BoxItemFilter>  void setBoxItemFilter(E filter) {
            mArgs.putSerializable(ARG_BOX_ITEM_FILTER, filter);
        }

        public BoxBrowseFolderFragment buildInstance() {
            BoxBrowseFolderFragment folderFragment = new BoxBrowseFolderFragment();
            folderFragment.setArguments(mArgs);
            return folderFragment;
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mFolder = (BoxFolder)savedInstanceState.getSerializable(OUT_ITEM);
        }
        if (getArguments() != null) {
            String folderId = getArguments().getString(ARG_ID, BoxConstants.ROOT_FOLDER_ID);
            String folderName = getArguments().getString(ARG_NAME);
            if (mFolder == null){
                mFolder = BoxFolder.createFromIdAndName(folderId, folderName);
            }

            if (SdkUtils.isBlank(mUserId)) {
                Toast.makeText(getActivity(), R.string.box_browsesdk_cannot_view_folder, Toast.LENGTH_LONG).show();
                // TODO: Call error handler
            }
        }
    }

    @Override
    public void onResume() {
        if (getArguments() != null){
            String folderName = getArguments().getString(ARG_NAME);
            setToolbar(folderName);
        }
        super.onResume();
    }

    @Override
    protected void loadItems() {
        mController.execute(mController.getFolderWithAllItems(mFolder.getId(), mCompletionListener));
    }

    /**
     *
     * @return the current folder this fragment is meant to display.
     */
    public BoxFolder getFolder(){
        return mFolder;
    }




    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mFolder = (BoxFolder) savedInstanceState.getSerializable(OUT_ITEM);
            if (mFolder != null && mFolder.getItemCollection() != null) {
                mAdapter.addAll(mFolder.getItemCollection());
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mFolder);
        super.onSaveInstanceState(outState);
    }


    protected void onOffsetItemsFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(CompletionListener.EXTRA_SUCCESS, false)) {
            checkConnectivity();
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.remove(intent.getAction());
        if (mFolder.getId().equals(intent.getStringExtra(CompletionListener.EXTRA_ID)) && intent.hasExtra(CompletionListener.EXTRA_FOLDER)) {
            mFolder = (BoxFolder)intent.getSerializableExtra(CompletionListener.EXTRA_FOLDER);
            super.onOffsetItemsFetched(intent);
        }
    }

    protected void onItemsFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null || mAdapter == null) {
            return;
        }

        if (!intent.getBooleanExtra(CompletionListener.EXTRA_SUCCESS, false)) {
            checkConnectivity();
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        if (mFolder.getId().equals(intent.getStringExtra(CompletionListener.EXTRA_ID))) {
            mFolder = (BoxFolder)intent.getSerializableExtra(CompletionListener.EXTRA_FOLDER);

            if (mFolder != null && mFolder.getName() != null) {
                getArguments().putString(ARG_NAME, mFolder.getName());
                this.setToolbar(mFolder.getName());
            }
            super.onItemsFetched(intent);
        }
    }

    public BoxApiFolder getFolderApi() {
        if (mFolderApi == null) {
            mFolderApi = new BoxApiFolder(mSession);
        }
        return mFolderApi;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an item being tapped to be communicated to the activity
     */
    public interface OnFragmentInteractionListener extends BoxBrowseFragment.OnFragmentInteractionListener{

        /**
         * Called whenever an item in the RecyclerView is tapped
         *
         * @param item the item that was tapped
         * @return whether the tap event should continue to be handled by the fragment
         */
        boolean handleOnItemClick(BoxItem item);
    }

}
