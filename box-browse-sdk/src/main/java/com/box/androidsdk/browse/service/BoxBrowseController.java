package com.box.androidsdk.browse.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.requests.BoxCacheableRequest;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.eclipsesource.json.JsonArray;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/***
 * Default implementation for the {@link BrowseController}.
 */
public class BoxBrowseController implements BrowseController {

    private static final String RECENT_SEARCHES_KEY = "BoxBrowseController.RecentSearchesKey";
    private static final int MAX_RECENT_SEARCHES = 10;
    private static final String TAG = BoxBrowseController.class.getName();


    // Static executors so that requests can be retained though activity/fragment lifecycle
    private static ThreadPoolExecutor mApiExecutor;
    private static ThreadPoolExecutor mThumbnailExecutor;

    protected final BoxApiFile mFileApi;
    protected final BoxApiFolder mFolderApi;
    protected final BoxApiSearch mSearchApi;
    protected final BoxSession mSession;
    protected final ThumbnailManager mThumbnailManager;
    protected BoxFutureTask.OnCompletedListener mListener;
    protected static final int BITMAP_CACHE_DEFAULT_SIZE = 10000;

    protected BitmapLruCache mThumbnailCache = new BitmapLruCache(BITMAP_CACHE_DEFAULT_SIZE);
    protected LruCache<Integer, Bitmap> mIconResCache = new LruCache<Integer, Bitmap>(10);


    /**
     * Instantiates a new Box browse controller.
     *
     * @param session   the session
     * @param apiFile   the api file
     * @param apiFolder the api folder
     * @param apiSearch the api search
     */
    public BoxBrowseController(BoxSession session, BoxApiFile apiFile, BoxApiFolder apiFolder, BoxApiSearch apiSearch) {
        mSession = session;
        mFileApi = apiFile;
        mFolderApi = apiFolder;
        mSearchApi = apiSearch;
        mThumbnailManager = createThumbnailManager(mSession);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        if (cacheSize < BITMAP_CACHE_DEFAULT_SIZE){
            System.out.println("xxx cacheSize " + cacheSize);
            mThumbnailCache = new BitmapLruCache(cacheSize);
        }
    }

    /**
     * Instantiates a new Box browse controller.
     *
     * @param session the session
     */
    public BoxBrowseController(BoxSession session) {
        mSession = session;
        mFileApi = new BoxApiFile(mSession);
        mFolderApi =  new BoxApiFolder(mSession);
        mSearchApi =  new BoxApiSearch(mSession);
        mThumbnailManager = createThumbnailManager(mSession);
    }

    private ThumbnailManager createThumbnailManager(BoxSession session) {
        try {
            return new ThumbnailManager(this);
        } catch (FileNotFoundException e) {
            BoxLogUtils.e(TAG, e);
        }
        return null;
    }

    @Override
    public BoxRequestsFolder.GetFolderWithAllItems getFolderWithAllItems(String folderId) {
        return mFolderApi.getFolderWithAllItems(folderId)
                .setFields(BoxFolder.ALL_FIELDS);
    }

    @Override
    public BoxRequestsSearch.Search getSearchRequest(String query) {
        return mSearchApi.getSearchRequest(query);
    }

    @Override
    public BoxRequestsFile.DownloadThumbnail getThumbnailRequest(String fileId, File downloadFile) {
        try {
            return mFileApi.getDownloadThumbnailRequest(downloadFile, fileId)
                    .setFormat(BoxRequestsFile.DownloadThumbnail.Format.JPG).
                            setMinSize(BoxRequestsFile.DownloadThumbnail.SIZE_160);
        } catch (IOException e) {
            BoxLogUtils.e(TAG, e);
        }
        return null;
    }

    @Override
    public void execute(BoxRequest request) {
        if (request == null) {
            return;
        }
        if (BoxConfig.getCache() != null && request instanceof BoxCacheableRequest){
            try {
                BoxFutureTask cacheTask = ((BoxCacheableRequest) request).toTaskForCachedResult();
                if (mListener != null){
                    cacheTask.addOnCompletedListener(mListener);
                }
                getApiExecutor().execute(cacheTask);
            } catch (BoxException e){
                BoxLogUtils.e("cache task error ", e);
            }

        }
        BoxFutureTask task = request.toTask();
        if (mListener != null) {
            task.addOnCompletedListener(mListener);
        }

        // Thumbnail request should be executed in their own executor pool
        ThreadPoolExecutor executor = request instanceof BoxRequestsFile.DownloadThumbnail ?
                getThumbnailExecutor() :
                getApiExecutor();
        executor.submit(task);
    }

    @Override
    public BrowseController setCompletedListener(BoxFutureTask.OnCompletedListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public void onError(Context context, BoxResponse response) {
        if (response.getRequest() instanceof BoxRequestsFolder.GetFolderWithAllItems) {
            Toast.makeText(context, R.string.box_browsesdk_problem_fetching_folder, Toast.LENGTH_LONG);
        } else if (response.getRequest() instanceof BoxRequestsSearch.Search) {
            Toast.makeText(context, R.string.box_browsesdk_problem_performing_search, Toast.LENGTH_LONG);
        }
    }

    @Override
    public File getThumbnailCacheDir() {

        // Create box thumbnail directory.
        // This should be same as in preview to ensure preview can use thumbnails from here
        File thumbnailDirectory = new File(mSession.getCacheDir(), "BoxThumbnails");
        if (!thumbnailDirectory.exists()) {
            thumbnailDirectory.mkdir();
        }

        return thumbnailDirectory;
    }

    @Override
    public ThumbnailManager getThumbnailManager() {
        return mThumbnailManager;
    }

    /**
     * Gets api executor.
     *
     * @return the api executor
     */
    protected ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }

    @Override
    public LruCache<File, Bitmap> getThumbnailCache() {
        return mThumbnailCache;
    }

    @Override
    public LruCache<Integer, Bitmap> getIconResourceCache() {
        return mIconResCache;
    }

    @Override
    public ArrayList<String> getRecentSearches(Context context, BoxUser user) {
        String recentSearchesString = context.getSharedPreferences(RECENT_SEARCHES_KEY + user.getId(), Context.MODE_PRIVATE).getString(RECENT_SEARCHES_KEY, null);
        ArrayList<String> recentSearches = new ArrayList<String>();

        if (recentSearchesString != null) {
            JsonArray recentSearchesJsonArray = JsonArray.readFrom(recentSearchesString);
            for (int i = 0; i < recentSearchesJsonArray.size(); i++) {
                recentSearches.add(recentSearchesJsonArray.get(i).asString());
            }
        }

        return recentSearches;
    }

    @Override
    public ArrayList<String> addToRecentSearches(Context context, BoxUser user, String recentSearch) {
        ArrayList<String> recentSearches = getRecentSearches(context, user);

        if (StringUtils.isEmpty(recentSearch)) {
            return recentSearches;
        }

        recentSearches.remove(recentSearch);

        if (recentSearches.size() >= MAX_RECENT_SEARCHES) {
            recentSearches.remove(recentSearches.size() - 1);
        }

        recentSearches.add(0, recentSearch);
        saveRecentSearches(context, user, recentSearches);
        return recentSearches;
    }

    @Override
    public ArrayList<String> deleteFromRecentSearches(Context context, BoxUser user, int indexToRemove) {
        ArrayList<String> recentSearches = getRecentSearches(context, user);
        recentSearches.remove(indexToRemove);
        saveRecentSearches(context, user, recentSearches);
        return recentSearches;
    }

    @Override
    public void saveRecentSearches(Context context, BoxUser user, ArrayList<String> searches) {
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < searches.size(); i++) {
            jsonArray.add(searches.get(i));
        }
        context.getSharedPreferences(RECENT_SEARCHES_KEY + user.getId(), Context.MODE_PRIVATE).edit().putString(RECENT_SEARCHES_KEY, jsonArray.toString()).commit();
    }

    /**
     * Executor that we will submit thumbnail tasks to.
     *
     * @return executor
     */
    @Override
    public ThreadPoolExecutor getThumbnailExecutor() {
        if (mThumbnailExecutor == null || mThumbnailExecutor.isShutdown()) {
            mThumbnailExecutor = new ThreadPoolExecutor(5, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
        }
        return mThumbnailExecutor;
    }

    @Override
    public void Log(String tag, String msg, Throwable t) {
        BoxLogUtils.e(tag, msg, t);
    }

    /**
     * The type Bitmap lru cache.
     */
    protected class BitmapLruCache extends LruCache<File, Bitmap> {

        /**
         * Instantiates a new Bitmap lru cache.
         *
         * @param sizeInKb the size in kb
         */
        public BitmapLruCache(int sizeInKb){
            super(sizeInKb);
        }

        @Override
        protected int sizeOf(File key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    }
}
