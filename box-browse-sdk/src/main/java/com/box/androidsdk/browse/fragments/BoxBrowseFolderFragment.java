package com.box.androidsdk.browse.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Use the {@link BoxBrowseFolderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxBrowseFolderFragment extends BoxBrowseFragment {

    protected BoxFolder mFolder = null;
    private static final String OUT_ITEM = "outItem";

    protected String mFolderId;
    protected String mFolderName;

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folder  the folder to browse
     * @param session the session that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(BoxFolder folder, BoxSession session) {
        return newInstance(folder.getId(), session.getUserId(), folder.getName());
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folderId the id of the folder to browse
     * @param userId   the id of the user that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(String folderId, String userId) {
        return newInstance(folderId, userId, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mFolder = (BoxFolder)savedInstanceState.getSerializable(OUT_ITEM);
        }
        if (getArguments() != null) {
            mFolderId = getArguments().getString(ARG_ID);
            mFolderName = getArguments().getString(ARG_NAME);
            if (mFolder == null){
                mFolder = BoxFolder.createFromId(mFolderId);
            }

            if (SdkUtils.isBlank(mFolderId) || SdkUtils.isBlank(mUserId)) {
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


    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folderId   the id of the folder to browse
     * @param userId     the id of the user that the contents will be loaded for
     * @param folderName the name of the folder that will be shown in the action bar
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(String folderId, String userId, String folderName) {
        BoxBrowseFolderFragment fragment = new BoxBrowseFolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, folderId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FutureTask<Intent> fetchInfo() {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_INFO);
                intent.putExtra(EXTRA_ID, mFolderId);
                try {
                    BoxRequestsFolder.GetFolderInfo req = new BoxApiFolder(mSession).getInfoRequest(mFolderId)
                            // TODO: Should clean-up to only include required fields
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
                intent.putExtra(EXTRA_ID, mFolderId);
                try {

                    // this call the collection is just BoxObjectItems and each does not appear to be an instance of BoxItem.
                    ArrayList<String> itemFields = new ArrayList<String>();
                    String[] fields = new String[]{BoxFile.FIELD_NAME, BoxFile.FIELD_SIZE, BoxFile.FIELD_OWNED_BY, BoxFolder.FIELD_HAS_COLLABORATIONS, BoxFolder.FIELD_IS_EXTERNALLY_OWNED};
                    BoxApiFolder api = new BoxApiFolder(mSession);
                    BoxListItems items = api.getItemsRequest(mFolderId).setLimit(limit).setOffset(offset).setFields(fields).send();
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
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.remove(intent.getAction());
        if (mFolderId.equals(intent.getStringExtra(EXTRA_ID))) {
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
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        if (mFolderId.equals(intent.getStringExtra(EXTRA_ID))) {
            mFolder = (BoxFolder)intent.getSerializableExtra(EXTRA_FOLDER);
            if (mFolder != null && mFolder.getName() != null) {
                getArguments().putString(ARG_NAME, mFolder.getName());
                this.setToolbar(mFolder.getName());
            }
            super.onInfoFetched(intent);
        }
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
