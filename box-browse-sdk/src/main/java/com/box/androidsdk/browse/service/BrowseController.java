package com.box.androidsdk.browse.service;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.collection.LruCache;

import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxRepresentation;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/***
 * Controller interface for the Box Browse SDK. This defines all of the requests that will be used by the SDK.
 */
public interface BrowseController {

    /***
     * Retrieves a folder with all its items. The response will be returned through the provided listener
     *
     * @param folderId the folder id
     * @return the folder with all items
     */
    BoxRequestsFolder.GetFolderWithAllItems getFolderWithAllItems(String folderId);

    /***
     * Retrieves search results for the given query
     *
     * @param query the query
     * @return search request
     */
    BoxRequestsSearch.Search getSearchRequest(String query);

    /***
     * Retrieves a thumbnail for a file. The response will be returned through the provided listener
     *
     * @param fileId       the file id
     * @param downloadFile the download file
     * @return thumbnail request
     */
    BoxRequestsFile.DownloadThumbnail getThumbnailRequest(String fileId, File downloadFile);

    /***
     * Retrieves a representation thumbnail for a file/Representation.
     * @param fileId          the file id
     * @param representation  the image representation to download
     * @param downloadFile    the file where the image will be saved
     * @return  a representation thumbnail request
     */
    BoxRequestsFile.DownloadRepresentation getRepresentationThumbnailRequest(String fileId,
                                                                             BoxRepresentation representation,
                                                                             File downloadFile);

    /***
     * Executes the request using the appropriate executor
     *
     * @param request the request
     */
    void execute(BoxRequest request);

    /***
     * Sets the default compeltion listener that will be used after the completion of a BoxRequest
     *
     * @param listener the listener
     * @return this
     */
    BrowseController setCompletedListener(BoxFutureTask.OnCompletedListener listener);

    /**
     * Error handler for whenever an error occurs from a request
     *
     * @param context  the context
     * @param response response returned from the server that contains the request, result, and exception
     */
    void onError(Context context, BoxResponse response);

    /**
     * Gets recent searches.
     *
     * @param context the context
     * @param user    the user
     * @return the recent searches
     */
    ArrayList<String> getRecentSearches(Context context, BoxUser user);

    /**
     * Add to recent searches array list.
     *
     * @param context      the context
     * @param user         the user
     * @param recentSearch the recent search
     * @return the array list
     */
    ArrayList<String> addToRecentSearches(Context context, BoxUser user, String recentSearch);

    /**
     * Delete from recent searches array list.
     *
     * @param context       the context
     * @param user          the user
     * @param indexToRemove the index to remove
     * @return the array list
     */
    ArrayList<String> deleteFromRecentSearches(Context context, BoxUser user, int indexToRemove);

    /**
     * Save recent searches.
     *
     * @param context  the context
     * @param user     the user
     * @param searches the searches
     */
    void saveRecentSearches(Context context, BoxUser user, ArrayList<String> searches);

    /**
     * Gets thumbnail cache dir.
     *
     * @return the thumbnail cache dir
     */
    File getThumbnailCacheDir();

    /**
     * Gets thumbnail manager.
     *
     * @return the thumbnail manager
     */
    ThumbnailManager getThumbnailManager();

    /**
     * Gets thumbnail cache.
     *
     * @return the thumbnail cache
     */
    LruCache<File, Bitmap> getThumbnailCache();

    /**
     * Gets icon resource cache.
     *
     * @return the icon resource cache
     */
    LruCache<Integer, Bitmap> getIconResourceCache();

    /**
     * Returns the executor used for thumbnail api requests
     *
     * @return thumbnail executor
     */
    ThreadPoolExecutor getThumbnailExecutor();

    /**
     * Use for any custom logging
     *
     * @param tag the tag
     * @param msg the msg
     * @param t   the t
     */
    void Log(String tag, String msg, Throwable t);
}
