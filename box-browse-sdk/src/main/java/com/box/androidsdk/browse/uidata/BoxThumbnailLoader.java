package com.box.androidsdk.browse.uidata;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxResponse;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

public class BoxThumbnailLoader extends BoxFutureTask<BoxDownload> {

    protected BoxThumbnailLoader(final Callable<BoxResponse<BoxDownload>> callable, final BoxRequest request) {
        super(callable, request);
    }

    public static BoxThumbnailLoader create(final BoxRequestsFile.DownloadThumbnail request, final ImageView target) {
        final WeakReference<ImageView> targetRef = new WeakReference<ImageView>(target);
        target.setTag(R.id.box_browsesdk_thumbnail_target_tag, request.getId());
        Callable<BoxResponse<BoxDownload>> callable = new Callable<BoxResponse<BoxDownload>>() {
            @Override
            public BoxResponse<BoxDownload> call() throws Exception {
                BoxDownload ret = null;
                Exception ex = null;
                try {
                    File imageFile = request.getTarget();
                    if (!imageFile.exists() || imageFile.length() == 0) {
                        // If the image has not been cached we make the remote call
                        ret = request.send();
                    }
                    final ImageView target = targetRef.get();
                    if (target != null && target.getTag(R.id.box_browsesdk_thumbnail_target_tag) == request.getId()) {
                        final Bitmap bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        target.post(new Runnable() {
                            @Override
                            public void run() {
                                target.setImageBitmap(bm);
                            }
                        });
                    }
                } catch (Exception e) {
                    ex = e;
                }
                return new BoxResponse<BoxDownload>(ret, ex, request);
            }
        };
        return new BoxThumbnailLoader(callable, request);
    }
}
