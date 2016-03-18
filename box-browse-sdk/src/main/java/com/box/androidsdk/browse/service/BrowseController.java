package com.box.androidsdk.browse.service;

import android.content.res.Resources;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import java.io.File;

/***
 * Controller interface for the Box Browse SDK. This defines all of the requests that will be used by the SDK.
 */
public interface BrowseController {

    /***
     * Retrieves a folder with all its items. The response will be returned through the provided listener
     *
     * @param folderId
     */
    BoxRequestsFolder.GetFolderWithAllItems getFolderWithAllItems(String folderId);

    /***
     * Retrieves search results for the given query
     *
     * @param query
     * @return
     */
    BoxRequestsSearch.Search getSearchRequest(String query);

    /***
     * Retrieves a thumbnail for a file. The response will be returned through the provided listener
     *
     * @param fileId
     * @param downloadFile
     * @param resources
     * @return
     */
    BoxRequestsFile.DownloadThumbnail getThumbnailRequest(String fileId, File downloadFile, Resources resources);

    /***
     * Executes the request using the appropriate executor
     *
     * @param request
     */
    void execute(BoxRequest request);

    /***
     * Sets the default compeltion listener that will be used after the completion of a BoxRequest
     *
     * @param listener
     * @return
     */
    BrowseController setCompletedListener(BoxFutureTask.OnCompletedListener listener);
}
