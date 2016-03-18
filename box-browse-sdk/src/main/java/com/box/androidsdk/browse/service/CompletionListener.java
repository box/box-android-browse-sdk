package com.box.androidsdk.browse.service;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;

import java.io.File;

public class CompletionListener implements BoxFutureTask.OnCompletedListener {

    public static final String EXTRA_SUCCESS = "com.box.androidsdk.browse.SUCCESS";
    public static final String EXTRA_EXCEPTION = "com.box.androidsdk.browse.EXCEPTION";
    public static final String EXTRA_ID = "com.box.androidsdk.browse.ID";
    public static final String EXTRA_FILE_ID = "com.box.androidsdk.browse.FILEID";
    public static final String EXTRA_OFFSET = "com.box.androidsdk.browse.OFFSET";
    public static final String EXTRA_LIMIT = "com.box.androidsdk.browse.LIMIT";
    public static final String EXTRA_FOLDER = "com.box.androidsdk.browse.FOLDER";
    public static final String EXTRA_COLLECTION = "com.box.androidsdk.browse.COLLECTION";

    private static final String TAG = CompletionListener.class.getName();

    private final LocalBroadcastManager mBroadcastManager;

    public CompletionListener(LocalBroadcastManager broadcastManager) {
        mBroadcastManager = broadcastManager;
    }

    @Override
    public void onCompleted(BoxResponse response) {
        if (response.isSuccess()) {
            Intent intent = new Intent();
            intent.setAction(response.getRequest().getClass().getName());
            intent.putExtra(EXTRA_SUCCESS, response.isSuccess());
            if (response.isSuccess()) {
                if (response.getRequest() instanceof BoxRequestsFolder.GetFolderWithAllItems) {
                    BoxFolder folder = (BoxFolder) response.getResult();
                    intent.putExtra(EXTRA_ID, folder.getId());
                    intent.putExtra(EXTRA_FOLDER, folder);
                    intent.putExtra(EXTRA_COLLECTION, folder.getItemCollection());
                } else if (response.getRequest() instanceof BoxRequestsSearch.Search) {
                    BoxIteratorItems items = (BoxIteratorItems) response.getResult();
                    intent.putExtra(EXTRA_OFFSET, items.offset());
                    intent.putExtra(EXTRA_LIMIT, items.limit());
                    intent.putExtra(CompletionListener.EXTRA_COLLECTION, items);
                } else if (response.getRequest() instanceof BoxRequestsFile.DownloadThumbnail) {
                    intent.putExtra(CompletionListener.EXTRA_FILE_ID, ((BoxRequestsFile.DownloadThumbnail) response.getRequest()).getId());
                    intent.putExtra(CompletionListener.EXTRA_SUCCESS, false);
                    File file = ((BoxDownload) response.getResult()).getOutputFile();

                    // Ensure the download file exists
                    if (file == null || !file.exists()) {
                        intent.putExtra(CompletionListener.EXTRA_SUCCESS, false);
                    }
                }
            }
            mBroadcastManager.sendBroadcast(intent);
        } else {
            BoxLogUtils.e(TAG, response.getException());
        }
    }

}
