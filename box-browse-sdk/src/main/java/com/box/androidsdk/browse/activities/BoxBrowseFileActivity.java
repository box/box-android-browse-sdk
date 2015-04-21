package com.box.androidsdk.browse.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxListItemAdapter;
import com.box.androidsdk.browse.adapters.NavigationListAdapter;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.NavigationItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class is used to choose a particular file from the user's account and when chosen will return a result with a BoxFile in the extra
 * BoxBrowseFileActivity.EXTRA_BOX_FILE
 */
public class BoxBrowseFileActivity extends BoxBrowseActivity {

    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFile} to the intent
     */
    public static final String EXTRA_BOX_FILE = "extraBoxFile";

    /**
     * Extra intent parameter used to display a folder name while the content is loading
     */
    public static final String EXTRA_FOLDER_NAME = "extraFolderName";

    /**
     * Extra intent parameter used to filter the returned files by extension type
     */
    public static final String EXTRA_BOX_EXTENSION_FILTER = "extraBoxExtensionFilter";

    /**
     * Extra serializable parameter that adds a {@link com.box.androidsdk.content.models.BoxFolder} to the saved instance state
     */
    protected static final String EXTRA_BOX_FOLDER = "extraBoxFolder";

    protected String mFolderName;

    protected ArrayList<String> mAllowedExtensions;

    protected BoxFolder mCurrentFolder = null;

    private final int FILE_PICKER_REQUEST_CODE = 4;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            mFolderName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);
            mAllowedExtensions = getIntent().getStringArrayListExtra(EXTRA_BOX_EXTENSION_FILTER);
        }
        if (savedInstanceState != null) {
            mCurrentFolder = (BoxFolder) savedInstanceState.getSerializable(EXTRA_BOX_FOLDER);
            mFolderName = savedInstanceState.getString(EXTRA_FOLDER_NAME);
            mAllowedExtensions = savedInstanceState.getStringArrayList(EXTRA_BOX_EXTENSION_FILTER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrentFolder == null && mFolderName != null) {
            setNavigationSpinner(new NavigationItem(mFolderName, mCurrentFolderId));
        }
        setNavigationSpinner(mCurrentFolder);
    }

    /**
     * Return the spinner used to navigate up folder hierarchy.
     */
    protected Spinner getNavigationSpinner() {
        return (Spinner) this.findViewById(R.id.folderChooserSpinner);
    }

    @Override
    protected BoxListItemAdapter initializeBoxListItemAdapter(ThumbnailManager thumbNailManager) {
        return new FilePickerBoxListItemAdapter(this, thumbNailManager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // if someone further down the chain has chosen a file then send result back.
                setResult(resultCode, data);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Opens a new folder activity with given folderId.
     *
     * @param folderId The folderId to navigate to.
     */
    private void openFolder(final String folderId) {
        if (folderId == null || mCurrentFolder != null && mCurrentFolder.getId().equals(folderId)) {
            return;
        }
        Intent intent = getLaunchIntent(this, folderId, mSession);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_NAV_NUMBER, (mNavNumber + 1));
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
    }

    /**
     * Opens a new folder activity with given folderId and folder name.
     *
     * @param folderName name of folder to open.
     * @param folderId   The folderId to navigate to.
     */
    private void openFolder(final String folderName, final String folderId) {
        if (folderId == null || mCurrentFolder != null && mCurrentFolder.getId().equals(folderId)) {
            return;
        }

        Intent intent = getLaunchIntent(this, folderName, folderId, mSession);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_NAV_NUMBER, (mNavNumber + 1));
        intent.putStringArrayListExtra(EXTRA_BOX_EXTENSION_FILTER, mAllowedExtensions);

        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_BOX_FOLDER, mCurrentFolder);
        outState.putString(EXTRA_FOLDER_NAME, mFolderName);
        outState.putStringArrayList(EXTRA_BOX_EXTENSION_FILTER, mAllowedExtensions);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void initializeViews() {
        setContentView(R.layout.boxsdk_layout_file_picker);
        TextView customTitle = (TextView) this.findViewById(R.id.customTitle);
        customTitle.setText(getTitle());
        // to make dialog theme fill the full view.
        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    }

    @Override
    protected ListView getListView() {
        return (ListView) this.findViewById(R.id.PickerListView);
    }

    @Override
    protected void handleFileClick(final BoxFile file) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BOX_FILE, file);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onFetchedFolder(Intent intent) {
        if (intent.getBooleanExtra(Controller.ARG_SUCCESS, false)) {
            if (mCurrentFolder == null) {
                mCurrentFolder = (BoxFolder) intent.getSerializableExtra(Controller.ARG_BOX_FOLDER);
            }
            setNavigationSpinner(mCurrentFolder);
        }
        super.onFetchedFolder(intent);
    }

    @Override
    protected void handleFolderClick(BoxFolder folder) {
        openFolder(folder.getName(), folder.getId());

    }

    private static String getFileExtension(final String fileName, final String defaultValue) {
        if (!fileName.contains(".")) {
            return defaultValue;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    protected boolean isItemEnabled(BoxItem item) {
        if (mAllowedExtensions == null || item instanceof BoxFolder) {
            return true;
        }
        return (item instanceof BoxFile) && mAllowedExtensions.contains(getFileExtension(item.getName(), ""));
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders.
     *
     * @param context  current context.
     * @param folderId folder id to navigate.
     * @param session  session.
     * @return an intent to launch an instance of this activity.
     */
    public static Intent getLaunchIntent(Context context, final String folderId, final BoxSession session) {
        Intent intent = BoxBrowseActivity.getLaunchIntent(context, folderId, session);
        intent.setClass(context, BoxBrowseFileActivity.class);
        return intent;
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
    public static Intent getLaunchIntent(Context context, final String folderName, final String folderId, final BoxSession session) {
        Intent intent = getLaunchIntent(context, folderId, session);
        intent.putExtra(EXTRA_FOLDER_NAME, folderName);
        return intent;
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will disable all files with extension types not included in the extensions list.
     *
     * @param context           current context.
     * @param folderId          folder id to navigate.
     * @param allowedExtensions extension types to enable.
     * @param session           session.
     * @return an intent to launch an instance of this activity.
     */
    public static Intent getLaunchIntent(Context context, final String folderId, final ArrayList<String> allowedExtensions, final BoxSession session) {
        Intent intent = BoxBrowseActivity.getLaunchIntent(context, folderId, session);
        intent.setClass(context, BoxBrowseFileActivity.class);
        intent.putStringArrayListExtra(EXTRA_BOX_EXTENSION_FILTER, allowedExtensions);
        return intent;
    }

    /**
     * Shows a single navigation item in the spinner.
     *
     * @param navigationItem The navigation item to show.
     */
    protected void setNavigationSpinner(final NavigationItem navigationItem) {
        if (navigationItem == null) {
            return;
        }

        ArrayList<NavigationItem> navigationFolders = new ArrayList<NavigationItem>();
        navigationFolders.add(navigationItem);
        Spinner spinner = getNavigationSpinner();
        spinner.setAdapter(new NavigationListAdapter(this, navigationFolders));
    }

    /**
     * Shows a navigation spinner with all items in the provided folder.
     *
     * @param currentFolder The folder to show in the navigation spinner.
     */
    protected void setNavigationSpinner(final BoxFolder currentFolder) {
        if (currentFolder == null) {
            return;
        }

        ArrayList<NavigationItem> navigationFolders = new ArrayList<NavigationItem>();
        navigationFolders.add(new NavigationItem(currentFolder.getName(), currentFolder.getId()));
        Iterator<BoxFolder> it = currentFolder.getPathCollection().iterator();
        while (it.hasNext()) {
            BoxFolder folder = it.next();
            navigationFolders.add(new NavigationItem(folder.getName(), folder.getId()));
        }
        Spinner spinner = getNavigationSpinner();
        spinner.setAdapter(new NavigationListAdapter(this, navigationFolders));
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                NavigationItem item = (NavigationItem) parent.getItemAtPosition(pos);
                openFolder(item.getFolderId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });
    }

    /**
     * Adapter that disables certain types of files
     */
    protected class FilePickerBoxListItemAdapter extends BoxBrowseActivity.FolderNavigationBoxListItemAdapter {

        public FilePickerBoxListItemAdapter(Activity context, ThumbnailManager manager) {
            super(context, manager);
        }

        @Override
        public boolean isEnabled(int position) {
            BoxItem item = getItem(position).getBoxItem();
            return item != null && isItemEnabled(item);
        }

        @Override
        protected void update(ViewHolder holder, BoxListItem listItem) {
            super.update(holder, listItem);
            if (!isItemEnabled(listItem.getBoxItem())) {
                holder.getNameView().setAlpha(0.5f);
                if (holder.getDescriptionView() != null) holder.getDescriptionView().setAlpha(0.5f);
                if (holder.getIconView() != null) holder.getIconView().setAlpha(0.5f);
            } else {
                holder.getNameView().setAlpha(1f);
                if (holder.getDescriptionView() != null) holder.getDescriptionView().setAlpha(1f);
                if (holder.getIconView() != null) holder.getIconView().setAlpha(1f);
            }
        }
    }
}
