package com.box.androidsdk.browse.service;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.utils.BoxLogUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BoxBrowseController implements BrowseController {
    private static final String TAG = BoxBrowseController.class.getName();

    // Static executors so that requests can be retained though activity/fragment lifecycle
    private static ThreadPoolExecutor mApiExecutor;
    private static ThreadPoolExecutor mThumbnailExecutor;

    private final BoxApiFile mFileApi;
    private final BoxApiFolder mFolderApi;
    private final BoxApiSearch mSearchApi;


    public BoxBrowseController(BoxApiFile apiFile, BoxApiFolder apiFolder, BoxApiSearch apiSearch) {
        mFileApi = apiFile;
        mFolderApi = apiFolder;
        mSearchApi = apiSearch;
    }

    @Override
    public FutureTask getFolderWithAllItems(String folderId, BoxFutureTask.OnCompletedListener listener) {
        BoxFutureTask task = mFolderApi
                .getFolderWithAllItems(folderId)
                .setFields(BoxFolder.ALL_FIELDS)
                .toTask()
                .addOnCompletedListener(listener);
        return task;
    }

    @Override
    public FutureTask getFolderItems(String folderId, final int offset, final int limit, BoxFutureTask.OnCompletedListener listener) {
        BoxFutureTask task = mFolderApi
                .getItemsRequest(folderId)
                .setLimit(limit)
                .setOffset(offset)
                .toTask()
                .addOnCompletedListener(listener);
        return task;
    }

    @Override
    public BoxRequestsSearch.Search getSearchRequest(String query) {
        return mSearchApi.getSearchRequest(query);
    }

    public FutureTask getSearchResults(String query, int offset, int limit, BoxFutureTask.OnCompletedListener listener) {
        BoxFutureTask task = mSearchApi.getSearchRequest(query)
                .setOffset(offset)
                .setLimit(limit)
                .toTask()
                .addOnCompletedListener(listener);
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            search.limitValueForKey(entry.getKey(), entry.getValue());
//        }
        return task;
    }

    @Override
    public BoxRequestsFile.DownloadThumbnail getThumbnailRequest(String fileId, File downloadFile, int width, int height) {
        try {
            return mFileApi.getDownloadThumbnailRequest(downloadFile, fileId)
                    .setMinWidth(width)
                    .setMinHeight(height);
        } catch (IOException e) {
            BoxLogUtils.e(TAG, e.getMessage());
        }
        return null;
    }


    public FutureTask getFileThumbnail(String fileId, File downloadLocation, int width, int height, BoxFutureTask.OnCompletedListener listener) {
        BoxFutureTask task = null;
        try {
            task = mFileApi.getDownloadThumbnailRequest(downloadLocation, fileId)
                    .setMinHeight(height)
                    .setMinWidth(width)
                    .toTask()
                    .addOnCompletedListener(listener);
        } catch (IOException e) {
            BoxLogUtils.e(TAG, e.getMessage());
        }
        return task;
    }

    @Override
    public void execute(FutureTask task) {
        getApiExecutor().submit(task);
    }

    protected ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }

    /**
     * Executor that we will submit thumbnail tasks to.
     *
     * @return executor
     */
    @Override
    public ThreadPoolExecutor getThumbnailExecutor() {
        if (mThumbnailExecutor == null || mThumbnailExecutor.isShutdown()) {
            mThumbnailExecutor = new ThreadPoolExecutor(1, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mThumbnailExecutor;
    }
}
