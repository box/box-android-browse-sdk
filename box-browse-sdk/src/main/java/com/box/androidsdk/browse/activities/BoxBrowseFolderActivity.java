package com.box.androidsdk.browse.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.fragments.BoxCreateFolderFragment;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIterator;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.net.HttpURLConnection;


public class BoxBrowseFolderActivity extends BoxBrowseActivity implements View.OnClickListener, BoxCreateFolderFragment.OnCreateFolderListener {

    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFolder} to the intent
     */
    public static final String EXTRA_BOX_FOLDER = "extraBoxFolder";

    protected Button mSelectFolderButton;

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

        Intent intent = new Intent(context, BoxBrowseFolderActivity.class);
        intent.putExtra(EXTRA_ITEM, folder);
        intent.putExtra(EXTRA_USER_ID, session.getUser().getId());
        return intent;
    }

    /**
     * Create a builder object that can be used to construct an intent to launch an instance of this activity.
     *
     * @param context current context.
     * @param session a session, should be already authenticated.
     * @return a builder object to use to construct an instance of this class.
     */
    public static BoxBrowseFolderActivity.IntentBuilder createIntentBuilder(final Context context, final BoxSession session) {
        return new IntentBuilder(context, session);
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders.
     *
     * @param context  current context.
     * @param folderId folder id to navigate.
     * @param session  session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderId, final BoxSession session) {
        return createIntentBuilder(context, session).setStartingFolder(BoxFolder.createFromId(folderId)).createIntent();
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will immediately show the given name in the navigation spinner
     * to before information about it has been fetched from the server.
     *
     * @param context    current context.
     * @param folderName Name to show in the navigation spinner. Should be name of the folder.
     * @param folderId   folder id to navigate.
     * @param session    session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderName, final String folderId, final BoxSession session) {

        BoxFolder folder = BoxFolder.createFromId(folderId);
        JsonObject jsonObject = folder.toJsonObject();
        jsonObject.add(BoxItem.FIELD_NAME, folderName);
        folder = new BoxFolder(jsonObject);
        return createIntentBuilder(context, session).setStartingFolder(folder).createIntent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_folder);

        mSelectFolderButton = (Button) findViewById(R.id.box_browsesdk_select_folder_button);
        mSelectFolderButton.setOnClickListener(this);
        initToolbar();
        if (getSupportFragmentManager().getBackStackEntryCount() < 1) {
            onItemClick(mItem);
        }
        mSelectFolderButton.setEnabled(true);

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        BoxFolder curFolder = getCurrentFolder();
        if (curFolder != null) {
            JsonObject jsonObject = curFolder.toJsonObject();
            JsonValue obj = jsonObject.get(BoxFolder.FIELD_ITEM_COLLECTION);
            if (obj != null && !obj.isNull()) {
                obj.asObject().set(BoxIterator.FIELD_ENTRIES, new JsonArray());
            }
            intent.putExtra(EXTRA_BOX_FOLDER, new BoxFolder(jsonObject));
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void handleBoxResponse(BoxResponse response) {
        if (response.isSuccess()) {
            if (response.getRequest() instanceof BoxRequestsFolder.CreateFolder) {
                BoxBrowseFragment browseFrag = getTopBrowseFragment();
                if (browseFrag != null) {
                    browseFrag.onRefresh();
                }

            }
        } else {
            int resId = R.string.box_browsesdk_network_error;
            if (response.getException() instanceof BoxException) {
                if (((BoxException) response.getException()).getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                    resId = R.string.box_browsesdk_create_folder_conflict;
                } else {

                }
            }
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest) {
        // Search will be limited to folders only.
        searchRequest.limitType(BoxFolder.TYPE);
        return super.onSearchRequested(searchRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_folder, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.box_browsesdk_action_create_folder) {
            BoxCreateFolderFragment.newInstance(getCurrentFolder(), mSession)
                    .show(getFragmentManager(), TAG);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateFolder(String name) {
        BoxApiFolder folderApi = new BoxApiFolder(mSession);
        BoxRequestsFolder.CreateFolder req = folderApi.getCreateRequest(getCurrentFolder().getId(), name);
        getApiExecutor(getApplication()).execute(req.toTask());
    }

    /**
     * An IntentBuilder used to create an intent to launch an instance of this class. Use this to add more
     * complicated logic to your activity beyond the simple getLaunchIntent method.
     */
    public static class IntentBuilder extends BoxBrowseActivity.IntentBuilder<IntentBuilder> {


        protected IntentBuilder(final Context context, final BoxSession session) {
            super(context, session);
        }

        @Override
        protected Intent createLaunchIntent() {
            if (mFolder == null) {
                mFolder = BoxFolder.createFromId("0");
            }
            return getLaunchIntent(mContext, mFolder, mSession);
        }
    }

}
