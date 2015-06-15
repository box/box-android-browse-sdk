package com.box.androidsdk.sample;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.box.androidsdk.browse.activities.BoxBrowseFileActivity;
import com.box.androidsdk.browse.activities.BoxBrowseFolderActivity;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;

import java.io.File;


public class SampleBrowseActivity extends ActionBarActivity {

    private static final int REQUEST_CODE_FILE_PICKER = 1;
    private static final int REQUEST_CODE_FOLDER_PICKER = 2;

    private Button btnFilePicker;
    private Button btnFolderPicker;

    private BoxSession session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_browse);
        BoxConfig.CLIENT_ID = "your-client-id";
        BoxConfig.CLIENT_SECRET = "your-client-secret";
        initUI();

        session = new BoxSession(this);
        session.authenticate();
    }

    private void launchFilePicker() {
        startActivityForResult(BoxBrowseFileActivity.getLaunchIntent(this, BoxFolder.createFromId("0"), session), REQUEST_CODE_FILE_PICKER);
    }

    private void launchFolderPicker() {
        startActivityForResult(BoxBrowseFolderActivity.getLaunchIntent(this, BoxFolder.createFromId("0"), session), REQUEST_CODE_FOLDER_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    BoxFile boxFile = (BoxFile) data.getSerializableExtra(BoxBrowseFileActivity.EXTRA_BOX_FILE);
                    Toast.makeText(this, String.format("File picked, id: %s; name: %s", boxFile.getId(), boxFile.getName()), Toast.LENGTH_LONG).show();
                } else {
                    // No file selected
                }
                break;
            case REQUEST_CODE_FOLDER_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    BoxFolder boxFolder = (BoxFolder) data.getSerializableExtra(BoxBrowseFolderActivity.EXTRA_BOX_FOLDER);
                    Toast.makeText(this, String.format("Folder picked, id: %s; name: %s", boxFolder.getId(), boxFolder.getName()), Toast.LENGTH_LONG).show();
                } else {
                    // No folder selected
                }
                break;
            default:
        }
    }

    private void initUI() {
        btnFilePicker = (Button) findViewById(R.id.btnFilePicker);
        btnFolderPicker = (Button) findViewById(R.id.btnFolderPicker);
        btnFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFilePicker();
            }
        });
        btnFolderPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFolderPicker();
            }
        });
    }
}
