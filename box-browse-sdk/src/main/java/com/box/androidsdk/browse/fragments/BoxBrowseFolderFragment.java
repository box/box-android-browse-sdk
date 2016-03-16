package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.filters.BoxItemFilter;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

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

    /**
     *
     * @return the current folder this fragment is meant to display.
     */
    public BoxFolder getFolder(){
        return mFolder;
    }



    @Override
    public FutureTask<Intent> fetchInfo() {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_INFO);
                intent.putExtra(EXTRA_ID, mFolder.getId());
                try {
                    BoxRequestsFolder.GetFolderWithAllItems req = getFolderApi().getFolderWithAllItems(mFolder.getId())
                            .setFields(BoxFolder.ALL_FIELDS);
                    BoxFolder bf = req.send();
                    if (bf != null) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                        intent.putExtra(EXTRA_FOLDER, bf);
                        intent.putExtra(EXTRA_COLLECTION, bf.getItemCollection());
                    }
                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    public FutureTask<Intent> fetchItems(final int offset, final int limit) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_ITEMS);
                intent.putExtra(EXTRA_OFFSET, offset);
                intent.putExtra(EXTRA_LIMIT, limit);
                intent.putExtra(EXTRA_ID, mFolder.getId());
                try {

                    // this call the collection is just BoxObjectItems and each does not appear to be an instance of BoxItem.
                    ArrayList<String> itemFields = new ArrayList<String>();
                    String[] fields = new String[]{BoxFile.FIELD_NAME, BoxFile.FIELD_SIZE, BoxFile.FIELD_OWNED_BY, BoxFolder.FIELD_HAS_COLLABORATIONS, BoxFolder.FIELD_IS_EXTERNALLY_OWNED, BoxFolder.FIELD_PARENT};
                    BoxIteratorItems items = getFolderApi().getItemsRequest(mFolder.getId()).setLimit(limit).setOffset(offset).setFields(fields).send();
                    intent.putExtra(EXTRA_SUCCESS, true);
                    intent.putExtra(EXTRA_COLLECTION, items);
                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
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


    protected void onItemsFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            checkConnectivity();
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.remove(intent.getAction());
        if (mFolder.getId().equals(intent.getStringExtra(EXTRA_ID)) && intent.hasExtra(EXTRA_FOLDER)) {
            mFolder = (BoxFolder)intent.getSerializableExtra(EXTRA_FOLDER);
            super.onItemsFetched(intent);
        }
    }

    protected void onInfoFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null || mAdapter == null) {
            return;
        }

        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            checkConnectivity();
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        if (mFolder.getId().equals(intent.getStringExtra(EXTRA_ID))) {
            mFolder = (BoxFolder)intent.getSerializableExtra(EXTRA_FOLDER);

            if (mFolder != null && mFolder.getName() != null) {
                getArguments().putString(ARG_NAME, mFolder.getName());
                this.setToolbar(mFolder.getName());
            }
            super.onInfoFetched(intent);
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
