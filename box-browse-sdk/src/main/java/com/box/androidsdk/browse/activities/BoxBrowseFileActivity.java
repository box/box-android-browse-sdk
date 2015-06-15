package com.box.androidsdk.browse.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.fragments.BoxSearchFragment;
import com.box.androidsdk.browse.uidata.BoxSearchView;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BoxBrowseFileActivity extends BoxBrowseActivity implements BoxSearchView.OnBoxSearchListener{
    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFile} to the intent
     */
    public static final String EXTRA_BOX_FILE = "extraBoxFile";

    /**
     * Extra intent parameter used to filter the returned files by extension type
     */
    public static final String EXTRA_BOX_EXTENSION_FILTER = "extraBoxExtensionFilter";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_file);
        if (getSupportFragmentManager().getBackStackEntryCount() < 1){
            onBoxItemSelected(mItem);
        }
        initToolbar();
    }

    @Override
    public boolean handleOnItemClick(BoxItem item) {
        if (item instanceof BoxFile || item instanceof BoxBookmark) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_BOX_FILE, item);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return false;
        }
        else {
            onBoxItemSelected(item);
            return false;
        }
    }


    @Override
    public void onBoxItemSelected(BoxItem boxItem) {
        super.onBoxItemSelected(boxItem);
        if (!(boxItem instanceof BoxFolder)) {
            handleOnItemClick(boxItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_file, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest) {
        if(getIntent().getStringArrayListExtra(EXTRA_BOX_EXTENSION_FILTER) != null){
            ArrayList<String> list = getIntent().getStringArrayListExtra(EXTRA_BOX_EXTENSION_FILTER);
            String[] extensions =  list.toArray(new String[list.size()]);
            searchRequest.limitFileExtensions(extensions);
        }
        return super.onSearchRequested(searchRequest);
    }


    /**
     * Create an intent to launch an instance of this activity to browse folders.
     *
     * @param context current context.
     * @param folder  folder to browse
     * @param session a session, should be already authenticated.
     * @return an intent to launch an instance of this activity.
     */
    public static Intent getLaunchIntent(Context context, final BoxFolder folder, final BoxSession session) {
        if (folder == null || SdkUtils.isBlank(folder.getId()))
            throw new IllegalArgumentException("A valid folder must be provided to browse");
        if (session == null || session.getUser() == null || SdkUtils.isBlank(session.getUser().getId()))
            throw new IllegalArgumentException("A valid user must be provided to browse");

        Intent intent = new Intent(context, BoxBrowseFileActivity.class);
        intent.putExtra(EXTRA_ITEM, folder);
        intent.putExtra(EXTRA_USER_ID, session.getUser().getId());
        return intent;
    }


    /**
     * Create a builder object that can be used to construct an intent to launch an instance of this activity.
     * @param context current context.
     * @param session a session, should be already authenticated.
     * @return a builder object to use to construct an instance of this class.
     */
    public static BoxBrowseFileActivity.IntentBuilder createIntentBuilder(final Context context, final BoxSession session){
        return new IntentBuilder(context, session);
    }


    public static class IntentBuilder extends BoxBrowseActivity.IntentBuilder<IntentBuilder>{

        ArrayList<String> mAllowedExtensions;

        protected IntentBuilder(final Context context, final BoxSession session){
            super(context, session);
        }

        public IntentBuilder setAllowedExtensions(final ArrayList<String> allowedExtensions){
            mAllowedExtensions = allowedExtensions;
            return this;
        }

        @Override
        protected void addExtras(Intent intent) {
            if (mAllowedExtensions != null){
                intent.putExtra(EXTRA_BOX_EXTENSION_FILTER, mAllowedExtensions);
            }
            super.addExtras(intent);
        }

        @Override
        protected Intent createLaunchIntent() {
            if (mFolder == null){
                mFolder = BoxFolder.createFromId("0");
            }
            return getLaunchIntent(mContext, mFolder, mSession);
        }


    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders.
     * This method is deprecated use the createIntentBuilder instead.
     *
     * @param context  current context.
     * @param folderId folder id to navigate.
     * @param session  session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderId, final BoxSession session) {
        return createIntentBuilder(context,session).setStartingFolder(BoxFolder.createFromId(folderId)).createIntent();
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will immediately show the given name in the navigation spinner
     * to before information about it has been fetched from the server.
     * This method is deprecated use the createIntentBuilder instead.
     * @param context    current context.
     * @param folderName Name to show in the navigation spinner. Should be name of the folder.
     * @param folderId   folder id to navigate.
     * @param session    session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderName, final String folderId, final BoxSession session) {
        LinkedHashMap<String, Object> folderMap = new LinkedHashMap<String, Object>();
        folderMap.put(BoxItem.FIELD_ID, folderId);
        folderMap.put(BoxItem.FIELD_TYPE, BoxFolder.TYPE);
        folderMap.put(BoxItem.FIELD_NAME, folderName);
        return createIntentBuilder(context,session).setStartingFolder(new BoxFolder(folderMap)).createIntent();
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will disable all files with extension types not included in the extensions list.
     * This method is deprecated use the createIntentBuilder instead.
     * @param context           current context.
     * @param folderId          folder id to navigate.
     * @param allowedExtensions extension types to enable.
     * @param session           session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderId, final ArrayList<String> allowedExtensions, final BoxSession session) {
       return createIntentBuilder(context,session).setStartingFolder(BoxFolder.createFromId(folderId)).setAllowedExtensions(allowedExtensions).createIntent();
    }

}
