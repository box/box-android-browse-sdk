package com.box.androidsdk.browse.uidata;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.requests.BoxRequest;
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
    public static LoaderDrawable create(BoxRequestsFile.DownloadThumbnail request, ImageView imageView, Bitmap placeHolder) {
        return new LoaderDrawable(ThumbnailTask.create(request, imageView),imageView.getResources(), placeHolder);
    }

    private static class ThumbnailTask extends BoxFutureTask<BoxDownload> {

        static Handler mMainHandler = new Handler(Looper.getMainLooper());
        private final String mKey;

        protected ThumbnailTask(final Callable<BoxResponse<BoxDownload>> callable, final BoxRequest request) {
            super(callable, request);
            mKey = createRequestKey(request);
        }

        private static String createRequestKey(BoxRequest req) {
            if (req instanceof BoxRequestsFile.DownloadThumbnail) {
                return ((BoxRequestsFile.DownloadThumbnail) req).getId();
            }
            return Integer.toString(req.hashCode());
        }

        public String getKey() {
            return mKey;
        }

        public static ThumbnailTask create(final BoxRequestsFile.DownloadThumbnail request, final ImageView target) {
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
                        final Bitmap bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                        // Ensure that the image view has not been recycled before setting the image
                        final String key = createRequestKey(request);
                        if (target != null && target.getDrawable() instanceof LoaderDrawable &&
                                ((LoaderDrawable) target.getDrawable()).getTask().getKey() == key) {
                            setImage(target, bm, !isCached);
                        }
                    } catch (Exception e) {
                        ex = e;
                    }
                    return new BoxResponse<BoxDownload>(ret, ex, request);
                }
            };
            return new ThumbnailTask(callable, request);
        }

        public static void setImage(final ImageView target, final Bitmap bm, final boolean animate) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (animate) {
                        target.setAlpha(0f);
                        target.setImageBitmap(bm);
                        ViewPropertyAnimator animation = target.animate();
                        animation.alpha(1f).setDuration(400).start();
                    } else {
                        target.setImageBitmap(bm);
                    }
                }
            });
        }
    }

}
