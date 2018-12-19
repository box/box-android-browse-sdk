package com.box.androidsdk.browse.uidata;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestDownload;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxResponse;

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

    /**
     * Gets the thumbnail task that is held by this object
     *
     * @return the task
     */
    public ThumbnailTask getTask() {
        return mTaskRef.get();
    }

    /**
     * Creates a LoaderDrawable that is responsible for loading a thumbnail into the provided image view
     *
     * @param request            the request
     * @param boxItem            the box item
     * @param imageView          the image view
     * @param placeHolder        the place holder
     * @param imageReadyListener the image ready listener
     * @return loader drawable
     */
    public static LoaderDrawable create(BoxRequestsFile.DownloadThumbnail request, final BoxItem boxItem, ImageView imageView, Bitmap placeHolder, final ImageReadyListener imageReadyListener) {
        return new LoaderDrawable(ThumbnailTask.create(request, boxItem, imageView, imageReadyListener),imageView.getResources(), placeHolder);
    }

    public static LoaderDrawable create(BoxRequestsFile.DownloadRepresentation request, final BoxItem boxItem, ImageView imageView, Bitmap placeHolder, final ImageReadyListener imageReadyListener) {
        return new LoaderDrawable(ThumbnailTask.create(request, boxItem, imageView, imageReadyListener),imageView.getResources(), placeHolder);
    }

    /**
     * Checks to see if this loader drawable matches the given request.
     *
     * @param request a request for downloading a thumbnail.
     * @return boolean
     */
    public boolean matchesRequest(final BoxRequest request){
        if(getTask() != null){
            return getTask().getKey().equals(ThumbnailTask.createRequestKey(request));
        }
        return false;
    }

    /**
     * This class is a BoxFutureTask used for downloading thumbnails.
     */
    static class ThumbnailTask extends BoxFutureTask<BoxDownload> {

        private final String mKey;
        private final BoxItem mBoxItem;


        /**
         * Instantiates a new Thumbnail task.
         *
         * @param callable the callable
         * @param request  the request
         * @param boxItem  the box item
         */
        protected ThumbnailTask(final Callable<BoxResponse<BoxDownload>> callable, final BoxRequest request, final BoxItem boxItem) {
            super(callable, request);
            mKey = createRequestKey(request);
            mBoxItem = boxItem;
        }

        /**
         * Create request key string.
         *
         * @param req the req
         * @return the string
         */
        protected static String createRequestKey(BoxRequest req) {
            if (req instanceof BoxRequestsFile.DownloadThumbnail) {
                return ((BoxRequestsFile.DownloadThumbnail) req).getId();
            }
            if(req instanceof BoxRequestsFile.DownloadRepresentation) {
                return ((BoxRequestsFile.DownloadRepresentation) req).getId();
            }
            return Integer.toString(req.hashCode());
        }


        /**
         * Gets the key that identifies this request
         *
         * @return the key
         */
        public String getKey() {
            return mKey;
        }

        /**
         * Get box item for which thumbnail is being loaded
         *
         * @return the box item
         */
        public BoxItem getBoxItem(){
            return mBoxItem;
        }

        /**
         * Create a thumbnail task.
         *
         * @param request            the request
         * @param boxItem            the box item
         * @param targetView             the target
         * @param imageReadyListener the image ready listener
         * @return the thumbnail task
         */
        public static ThumbnailTask create(final BoxRequestDownload request, final BoxItem boxItem, ImageView targetView, final ImageReadyListener imageReadyListener) {
            final WeakReference<ImageView> targetRef = new WeakReference<ImageView>(targetView);
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
                            ret = (BoxDownload)request.send();
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
                    BoxResponse response = new BoxResponse<BoxDownload>(ret, ex, request);
                    if (ex != null){
                        imageReadyListener.onImageException(response, targetRef.get());
                    }
                    return response;
                }
            };
            return new ThumbnailTask(callable, request, boxItem);
        }
    }


    /**
     * Callback interface for signifying that the thumbnail has been loaded into the imageview.
     */
    public interface ImageReadyListener {

        /**
         * On image ready.
         *
         * @param bitmapSourceFile the bitmap source file
         * @param request          the request
         * @param bitmap           the bitmap
         * @param view             the view
         */
        void onImageReady(File bitmapSourceFile, BoxRequest request, Bitmap bitmap, ImageView view);

        /**
         * An exception occurred trying to load image.
         * @param response the server response containing request and exception info
         * @param view the view
         */
        void onImageException(BoxResponse response, ImageView view);
    }
}
