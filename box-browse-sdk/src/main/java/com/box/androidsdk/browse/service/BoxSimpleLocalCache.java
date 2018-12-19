package com.box.androidsdk.browse.service;

import com.box.androidsdk.content.BoxCache;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxObject;
import com.box.androidsdk.content.requests.BoxCacheableRequest;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxResponse;

import androidx.collection.LruCache;

/***
 * A very simple implementation of BoxCache that caches some folder responses in memory for faster retrieval.
 */
public class BoxSimpleLocalCache implements BoxCache {

    /**
     * Instantiates a new Box simple local cache.
     */
    public BoxSimpleLocalCache(){
    }

    /**
     * The full folder cache.
     */
    LruCache<String, BoxFolder> mFullFolderCache = new LruCache<String, BoxFolder>(10);

    @Override
    public <T extends BoxObject> void put(BoxResponse<T> response) throws BoxException {
        if (response.isSuccess() && response.getRequest() instanceof BoxRequestsFolder.GetFolderWithAllItems){
            mFullFolderCache.put(((BoxRequestsFolder.GetFolderWithAllItems) response.getRequest()).getId(), (BoxFolder)response.getResult());
        }
    }

    @Override
    public <T extends BoxObject, R extends BoxRequest & BoxCacheableRequest> T get(R request) throws BoxException {

        if (request instanceof BoxRequestsFolder.GetFolderWithAllItems){
            // Assume fields have not changed.
            return (T)mFullFolderCache.get(((BoxRequestsFolder.GetFolderWithAllItems) request).getId());
        }
        return null;
    }
}
