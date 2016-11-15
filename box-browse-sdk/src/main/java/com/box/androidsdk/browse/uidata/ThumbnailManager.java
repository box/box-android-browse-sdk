package com.box.androidsdk.browse.uidata;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ListView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;


/**
 * This class manages thumbnails to display to users. This class does not do network calls.
 */
public class ThumbnailManager implements LoaderDrawable.ImageReadyListener{

    /** The extension added for thumbnails in this manager. */
    private static final String THUMBNAIL_FILE_EXTENSION = ".thumbnail";

    /** Controller used for all requests */
    private final BrowseController mController;

    /**
     * Maps the target image view to the thumbnail task. Provides ability to cancel tasks
     */
    WeakHashMap<Object, BoxFutureTask> mTargetToTask = new WeakHashMap<Object, BoxFutureTask>();

    protected final static HashMap<String, Integer> DEFAULT_ICON_RESORCE_MAP = new HashMap<String, Integer>();

    public static final String[] DOCUMENTS_EXTENSIONS_ARRAY = {"csv", "doc", "docx", "gdoc", "gsheet", "htm", "html", "msg", "odp", "odt", "ods", "pdf",
            "ppt", "pptx", "rtf", "tsv", "wpd", "xhtml", "xls", "xlsm", "xlsx", "xml", "xsd", "xsl", "txt"};
    public static final String[] PRESENTATION_EXTENSIONS_ARRAY = {"ppt", "pptx"};
    public static final String[] SPREADSHEET_EXTENSIONS_ARRAY = {"csv", "gsheet", "xls", "xlsm", "xlsx", "xsd", "xsl"};
    public static final String[] WORD_EXTENSIONS_ARRAY = {"doc", "docx"};
    public static final String[] AUDIO_EXTENSIONS_ARRAY = {"aac", "aif", "aifc", "aiff", "amr", "au", "flac", "m4a", "mp3", "ra", "wav", "wma"};
    public static final String[] CODE_EXTENSIONS_ARRAY = {"h", "c", "cp", "cpp", "c++", "cc", "cxx", "m", "strings", "hpp", "h++", "hxx", "mm", "java", "jav", "scala",
            "clj", "coffee", "cl", "css", "diff", "erl", "go", "groovy", "hs", "lhs", "hx", "asp", "aspx", "ejs", "jsp", "html", "htm", "js", "jscript", "javascript", "json",
            "ts", "less", "lua", "markdown", "mdown", "md", "mysql", "sql", "nt", "ocaml", "pas", "pp", "lpr", "dpr", "pascal", "pl", "php", "pig", "plsql", "properties", "ini",
            "py", "r", "rpm", "rst", "rb", "rs", "scheme", "sh", "siv", "sieve", "st", "smarty", "rq", "stex", "tiddlywiki", "vb", "frm", "cs", "vbs", "vm", "v", "vh", "xml",
            "xhtml", "xquery", "xq", "xqy", "yml", "yaml", "z80"};
    public static final String[] VIDEO_EXTENSIONS_ARRAY = {"3g2", "3gp", "avi", "m2v", "m2ts", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "ogg", "mts",
            "qt", "wmv"};
    public static final String[] COMPRESSED_EXTENSIONS_ARRAY = {"zip", "rar", "gz", "tar", "7z", "arc", "ace", "tbz"};
    public static final String[] INDESIGN_EXTENSIONS_ARRAY = {"indd", "indl", "indt", "indb", "inx", "idml", "pmd"};
    public static final String[] OBJ_EXTENSIONS_ARRAY = {"obj", "3ds", "x3d"};
    public static final String[] PHOTOSHOP_EXTENSIONS_ARRAY = {"psd", "psb"};
    public static final String[] VECTOR_EXTENSIONS_ARRAY = {"eps", "svg"};
    public static final String[] BOXNOTE_EXTENSIONS_ARRAY = {"boxnote"};
    public static final String[] IMAGE_EXTENSIONS_ARRAY = {"ai", "bmp", "dcm", "eps", "jpeg", "jpg", "png", "ps", "psd", "tif", "tiff", "svg", "gif", "ico"};
    public static final String[] PDF_EXTENSIONS_ARRAY = {"pdf"};

    protected static final ArrayList<String> IMAGE_EXTENSIONS = new ArrayList<String>();


    static {
        for (String ext : IMAGE_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_image);
            IMAGE_EXTENSIONS.add(ext);
        }
        for (String ext : DOCUMENTS_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_doc);
        }
        for (String ext : PRESENTATION_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_presentation);
        }
        for (String ext : SPREADSHEET_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_spreadsheet);
        }
        for (String ext : WORD_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_word);
        }
        for (String ext : AUDIO_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_audio);
        }
        for (String ext : COMPRESSED_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_compressed);
        }
        for (String ext : CODE_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_code);
        }
        for (String ext : VIDEO_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_movie);
        }
        for (String ext : INDESIGN_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_indesign);
        }
        for (String ext : OBJ_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_obj);
        }
        for (String ext : PHOTOSHOP_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_photoshop);
        }
        for (String ext : VECTOR_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_vector);
        }
        for (String ext : BOXNOTE_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_box_note);
        }
        for (String ext : PDF_EXTENSIONS_ARRAY){
            DEFAULT_ICON_RESORCE_MAP.put(ext, R.drawable.ic_box_browsesdk_pdf);
        }
        DEFAULT_ICON_RESORCE_MAP.put("ico", R.drawable.ic_box_browsesdk_icon);

        DEFAULT_ICON_RESORCE_MAP.put("ai", R.drawable.ic_box_browsesdk_illustrator);
    }


    /**
     * Constructor.
     *
     * @param controller the controller
     * @throws FileNotFoundException the file not found exception
     */
    public ThumbnailManager(BrowseController controller) throws FileNotFoundException {
        mController = controller;

        // Ensure that parent cache directory is present
        if (!mController.getThumbnailCacheDir().exists()) {
            mController.getThumbnailCacheDir().mkdirs();
        }

    }

    /**
     * Gets the default icon resource depending on what kind of boxItem is being viewed.
     *
     * @param boxItem The box item to show to user.
     * @return an integer resource.
     */
    public int getDefaultIconResource(final BoxItem boxItem) {
        if (boxItem instanceof BoxFolder) {
            BoxFolder boxFolder = (BoxFolder) boxItem;
            if (boxFolder.getHasCollaborations() == Boolean.TRUE) {
                if (boxFolder.getIsExternallyOwned() == Boolean.TRUE) {
                    return R.drawable.ic_box_browsesdk_folder_external;
                }
                return R.drawable.ic_box_browsesdk_folder_shared;
            }
            return R.drawable.ic_box_browsesdk_folder_personal;
        } else if (boxItem instanceof BoxBookmark) {
            return R.drawable.ic_box_browsesdk_web_link;
        } else {
            String name = boxItem.getName();
            if (name != null) {
                String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                Integer resId = DEFAULT_ICON_RESORCE_MAP.get(ext);
                if (resId != null){
                    return resId;
                }
            }
            return R.drawable.ic_box_browsesdk_other;
        }
    }

    /**
     * Returns a file in a determinate location for the given boxItem.
     *
     * @param boxItem the box item
     * @return a File object where the thumbnail is saved to or should be saved to.
     */
    public File getThumbnailForBoxItem(final BoxItem boxItem) {
        if (boxItem instanceof BoxFile) {
            return getThumbnailForBoxFile((BoxFile) boxItem);
        }

        return null;
    }

    /**
     * Returns a file in a determinate location for the given boxFile.
     *
     * @param boxFile box file.
     * @return a File object where the thumbnail is saved to or should be saved to.
     */
    public File getThumbnailForBoxFile(final BoxFile boxFile) {
        File file = new File(getThumbnailDirectory(), getCacheName(boxFile));
        try{
            file.createNewFile();

        } catch (IOException e){
            // Ignore errors in creating the file to store thumbnails to.
            mController.Log("getThumbnailForBoxFile ",  "file.getAbsolutePath()  " + file.getAbsolutePath() + " isFile " + file.isFile(), null);
            mController.Log("getThumbnailForBoxFile ", "file.getParentFile().exists() "
                    + Boolean.toString(file.getParentFile().exists()) + " isDirectory " + file.getParentFile().isDirectory(), null);
            mController.Log("getThumbnailForBoxFile ", "file.getParentFile().getParentFile.exists() " + Boolean.toString(
                    file.getParentFile().getParentFile().exists()) + " isDirectory " + file.getParentFile().getParentFile().isDirectory(), null);
            mController.Log("getThumbnailForBoxFile" , " IOException " , e);

        }
        return file;
    }

    /**
     * Gets cache name.
     *
     * @param boxFile the box file
     * @return the cache name
     */
    protected String getCacheName(BoxFile boxFile) {
        if (boxFile == null || SdkUtils.isBlank(boxFile.getId()) || SdkUtils.isBlank(boxFile.getSha1())) {
            throw new IllegalArgumentException("BoxFile argument must not be null and must also contain an id and sha1");
        }
        return String.format(Locale.ENGLISH, "%s_%s%s", boxFile.getId(), boxFile.getSha1(), THUMBNAIL_FILE_EXTENSION);
    }

    /**
     * Gets thumbnail directory.
     *
     * @return the cacheDirectory of this thumbnail manager.
     */
    public File getThumbnailDirectory() {
        return mController.getThumbnailCacheDir();
    }

    /**
     * Convenience method to delete all files in the provided cache directory.
     */
    public void deleteFilesInCacheDirectory() {
        File[] files = mController.getThumbnailCacheDir().listFiles();
        if (files != null) {
            for (int index = 0; index < files.length; index++) {
                if (files[index].isFile()) {
                    files[index].delete();
                }
            }
        }
    }

    /**
     * Is thumbnail available boolean.
     *
     * @param item the item
     * @return the boolean
     */
    public static boolean isThumbnailAvailable(BoxItem item) {
        if (item == null || SdkUtils.isBlank(item.getName())) {
            return false;
        }

        int index = item.getName().lastIndexOf(".");
        if (index > 0) {
            return IMAGE_EXTENSIONS.contains(item.getName().substring(index + 1).toLowerCase());
        }
        return false;
    }

    /**
     * Loads the thumbnail for the provided BoxItem (if available) into the target image view
     *
     * @param item        the item
     * @param targetImage the target image
     */
    public void loadThumbnail(final BoxItem item, final ImageView targetImage) {
        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (item instanceof BoxFile
                && item.getPermissions() != null
                && item.getPermissions().contains(BoxItem.Permission.CAN_PREVIEW)
                && isThumbnailAvailable(item)) {
            // Cancel pending task upon recycle.
            BoxFutureTask task = mTargetToTask.remove(targetImage);
            if (task != null) {
                task.cancel(false);
            }

            File thumbnailFile = getThumbnailForBoxFile((BoxFile) item);
            if (mController.getThumbnailCache() != null && mController.getThumbnailCache().get(thumbnailFile) != null){
                Bitmap bm = mController.getThumbnailCache().get(thumbnailFile);
                targetImage.setImageBitmap(mController.getThumbnailCache().get(thumbnailFile));
                return;
            }

            Bitmap placeHolderBitmap = null;
            int iconResId = getDefaultIconResource(item);
            if (mController.getIconResourceCache() != null){
                placeHolderBitmap = mController.getIconResourceCache().get(iconResId);
            }
            if (placeHolderBitmap == null) {
                if (targetImage.getMeasuredWidth() > 0 && targetImage.getMeasuredHeight() > 0) {
                    placeHolderBitmap = SdkUtils.decodeSampledBitmapFromFile(targetImage.getResources(), iconResId, targetImage.getMeasuredWidth(), targetImage.getMeasuredHeight());
                } else {
                    placeHolderBitmap = BitmapFactory.decodeResource(targetImage.getResources(), iconResId);
                }
                if ( mController.getIconResourceCache() != null) {
                    mController.getIconResourceCache().put(iconResId, placeHolderBitmap);
                }
            }

            // Set the drawable to our loader drawable, which will show a placeholder before loading the thumbnail into the view
            BoxRequestsFile.DownloadThumbnail request = mController.getThumbnailRequest(item.getId(), thumbnailFile);
            LoaderDrawable loaderDrawable = LoaderDrawable.create(request, item, targetImage, placeHolderBitmap, this);
            targetImage.setImageDrawable(loaderDrawable);
            BoxFutureTask thumbnailTask = loaderDrawable.getTask();
            if (thumbnailTask != null) {
                mTargetToTask.put(targetImage, thumbnailTask);
                mController.getThumbnailExecutor().execute(thumbnailTask);
            }
        } else {
            targetImage.setImageResource(getDefaultIconResource(item));
        }
    }

    @Override
    public void onImageReady(final File bitmapSourceFile, final BoxRequest request, final Bitmap bitmap, final ImageView view) {
        if (bitmap == null || bitmapSourceFile == null || view == null){
            return;
        }
        if (view.getMeasuredWidth() > 0 && view.getMeasuredHeight() > 0){
            mController.getThumbnailCache().put(bitmapSourceFile, ThumbnailUtils.extractThumbnail(bitmap, view.getMeasuredWidth(), view.getMeasuredHeight()));
            bitmap.recycle();
        } else {
            postLaterToView(bitmapSourceFile, request, bitmap, view);
            return;

        }
        Bitmap scaledBitmap = mController.getThumbnailCache().get(bitmapSourceFile);
        if (scaledBitmap != null && isRequestStillApplicable(request, view)){
            loadThumbnail(scaledBitmap, view);
        }
    }

    /**
     * Post later to view.
     *
     * @param bitmapSourceFile the bitmap source file
     * @param request          the request
     * @param bitmap           the bitmap
     * @param view             the view
     */
// this is a work around to get view to post this only view is attached. Must be done on ui thread.
    protected void postLaterToView(final File bitmapSourceFile, final BoxRequest request, final Bitmap bitmap, final ImageView view){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        mController.getThumbnailExecutor().execute(new Runnable(){
                            @Override
                            public void run() {
                                onImageReady(bitmapSourceFile, request, bitmap, view);
                            }
                        });
                    }
                });

            }
        });
    }

    /**
     * Load thumbnail.
     *
     * @param bitmap    the bitmap
     * @param imageView the image view
     */
    public void loadThumbnail(final Bitmap bitmap, final ImageView imageView){
        ViewParent parent = imageView.getParent();
        boolean isScrolling = false;
        while (parent != null){
            parent = parent.getParent();
            if (parent instanceof RecyclerView){
                isScrolling = ((RecyclerView) parent).getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
                if (isScrolling) {
                    final WeakReference<ImageView> imageViewRef = new WeakReference(imageView);
                    ((RecyclerView) parent).addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                            if (imageViewRef.get() == null){
                                recyclerView.removeOnScrollListener(this);
                                return;
                            }
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                ImageView view = imageViewRef.get();
                                if (view != null && view.getDrawable() instanceof LoaderDrawable) {
                                    loadThumbnail(((LoaderDrawable) view.getDrawable()).getTask().getBoxItem(), imageView);
                                }
                                recyclerView.removeOnScrollListener(this);

                            }
                        }
                    });
                }
            }
        }
        if (isScrolling){
            // do nothing we will handle this with the scroll listener.
        } else {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    //TODO decide whether to use two views for crossfading animation.
                    imageView.setImageBitmap(bitmap);
                }
            });
        }

    }



    private boolean isRequestStillApplicable(final BoxRequest request, final ImageView view){
        if (view.getDrawable() instanceof LoaderDrawable){
            return ((LoaderDrawable) view.getDrawable()).matchesRequest(request);
        }
        return false;

    }

}
