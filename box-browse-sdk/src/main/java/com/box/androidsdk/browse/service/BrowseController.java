package com.box.androidsdk.browse.service;

import android.content.Intent;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

public interface BrowseController {


    /***
     * Retrieves a folder with all its items. The response will be returned through the provided listener
     *  @param folderId
     * @param listener*/
    FutureTask getFolderWithAllItems(String folderId, BoxFutureTask.OnCompletedListener listener);

    /***
     * Retrieves a folder's items. The response will be returned through the provided listener
     * @param folderId
     * @param offset
     * @param limit
     * @param listener
     */
    FutureTask getFolderItems(String folderId, int offset, int limit, BoxFutureTask.OnCompletedListener listener);

    BoxRequestsSearch.Search getSearchRequest(String query);

    BoxRequestsFile.DownloadThumbnail getThumbnailRequest(String fileId, File downloadFile, int width, int height);

    /***
     * Retrieves a thumbnail for a file. The response will be returned through the provided listener
     *  @param fileId
     * @param downloadLocation
     * @param width
     * @param height
     * @param listener
     */
    FutureTask getFileThumbnail(String fileId, File downloadLocation, int width, int height, BoxFutureTask.OnCompletedListener listener);

    void execute(FutureTask task);

    ThreadPoolExecutor getThumbnailExecutor();
}
