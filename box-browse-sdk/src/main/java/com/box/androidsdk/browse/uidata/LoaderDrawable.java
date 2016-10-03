package com.box.androidsdk.browse.uidata;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * Container class that allows the thumbnail task to be held by the ImageView
 */
public class LoaderDrawable extends BitmapDrawable {

    WeakReference<ThumbnailTask> mTaskRef;

    private LoaderDrawable(ThumbnailTask task, Resources resources, Bitmap placeHolder) {
        super(resources, placeHolder);
        mTaskRef = new WeakReference<ThumbnailTask>(task);
    }

    public ThumbnailTask getTask() {
        return mTaskRef.get();
    }

    /**
     * Creates a LoaderDrawable that is responsible for loading a thumbnail into the provided image view
     *
     * @param request
     * @param imageView
     * @param placeHolder
     * @return
     */
    public static LoaderDrawable create(BoxRequestsFile.DownloadThumbnail request, final BoxItem boxItem, ImageView imageView, Bitmap placeHolder, final ImageReadyListener imageReadyListener) {
        return new LoaderDrawable(ThumbnailTask.create(request, boxItem, imageView, imageReadyListener),imageView.getResources(), placeHolder);
    }

    /**
     * Checks to see if this loader drawable matches the given request.
     * @param request a request for downloading a thumbnail.
     * @return
     */
    public boolean matchesRequest(final BoxRequest request){
        if(getTask() != null){
            return getTask().getKey().equals(ThumbnailTask.createRequestKey(request));
        }
        return false;
    }

    static class ThumbnailTask extends BoxFutureTask<BoxDownload> {

        private final String mKey;
        private final BoxItem mBoxItem;


        protected ThumbnailTask(final Callable<BoxResponse<BoxDownload>> callable, final BoxRequest request, final BoxItem boxItem) {
            super(callable, request);
            mKey = createRequestKey(request);
            mBoxItem = boxItem;
        }

        protected static String createRequestKey(BoxRequest req) {
            if (req instanceof BoxRequestsFile.DownloadThumbnail) {
                return ((BoxRequestsFile.DownloadThumbnail) req).getId();
            }
            return Integer.toString(req.hashCode());
        }



        public String getKey() {
            return mKey;
        }

        public BoxItem getBoxItem(){
            return mBoxItem;
        }

        public static ThumbnailTask create(final BoxRequestsFile.DownloadThumbnail request, final BoxItem boxItem, ImageView target, final ImageReadyListener imageReadyListener) {
            final WeakReference<ImageView> targetRef = new WeakReference<ImageView>(target);
            Callable<BoxResponse<BoxDownload>> callable = new Callable<BoxResponse<BoxDownload>>() {
                @Override
                public BoxResponse<BoxDownload> call() throws Exception {
                    BoxDownload ret = null;
                    Exception ex = null;
                    try {
                        File imageFile = request.getTarget();
                        boolean isCached = imageFile.exists() && imageFile.length() > 0;
                        if (!isCached) {
                            // If the image has not been cached we make the remote call
                            ret = request.send();
                        }
                        final ImageView target = targetRef.get();
                        Bitmap bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                        // Ensure that the image view has not been recycled before setting the image
                        final String key = createRequestKey(request);
                        if (bm != null && target != null && target.getDrawable() instanceof LoaderDrawable &&
                                ((LoaderDrawable) target.getDrawable()).getTask().getKey().equals(key)){
                            imageReadyListener.onImageReady(imageFile, request, bm, targetRef.get());
                        }
                    } catch (Exception e) {
                        ex = e;
                    }
                    return new BoxResponse<BoxDownload>(ret, ex, request);
                }
            };
            return new ThumbnailTask(callable, request, boxItem);
        }
    }


    public interface ImageReadyListener {

        void onImageReady(File bitmapSourceFile, BoxRequest request, Bitmap bitmap, ImageView view);

    }
}
