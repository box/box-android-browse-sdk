package com.box.androidsdk.browse.uidata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.SdkUtils;


/**
 * 
 * This class manages thumbnails to display to users. This class does not do network calls.
 */
public class ThumbnailManager {

    /** The extension added for thumbnails in this manager. */
    private static final String THUMBNAIL_FILE_EXTENSION = ".thumbnail";

    /** Controller used for all requests */
    private final BrowseController mController;

    /** The path where files in thumbnail should be stored. */
    private File mThumbnailDirectory;
    
    /** Executor that we will submit our set thumbnail tasks to. */
    private ThreadPoolExecutor thumbnailRequestExecutor;

    /** Maps the target image view to the thumbnail task. Provides ability to cancel tasks */
    WeakHashMap<Object, BoxFutureTask> mTargetToTask = new WeakHashMap<Object, BoxFutureTask>();

    private final static HashMap<String, Integer> DEFAULT_ICON_RESORCE_MAP = new HashMap<String, Integer>();

    protected static final String[] DOCUMENTS_EXTENSIONS_ARRAY = {"csv", "doc", "docx", "gdoc", "gsheet", "htm", "html", "msg", "odp", "odt", "ods", "pdf",
            "ppt", "pptx", "rtf", "tsv", "wpd", "xhtml", "xls", "xlsm", "xlsx", "xml", "xsd", "xsl"};
    protected static final String[] PRESENTATION_EXTENSIONS_ARRAY = {"ppt", "pptx"};
    protected static final String[] SPREADSHEET_EXTENSIONS_ARRAY = {"csv", "gsheet", "xls", "xlsm", "xlsx", "xsd", "xsl"};
    protected static final String[] WORD_EXTENSIONS_ARRAY = {"doc", "docx"};

    protected static final String[] AUDIO_EXTENSIONS_ARRAY = {"aac", "aif", "aifc", "aiff", "amr", "au", "flac", "m4a", "mp3", "ra", "wav", "wma"};
    protected static final String[] CODE_EXTENSIONS_ARRAY = {"h", "c", "cp", "cpp", "c++", "cc", "cxx", "m", "strings", "hpp", "h++", "hxx", "mm", "java", "jav", "scala",
            "clj", "coffee", "cl", "css", "diff", "erl", "go", "groovy", "hs", "lhs", "hx", "asp", "aspx", "ejs", "jsp", "html", "htm", "js", "jscript", "javascript", "json",
            "ts", "less", "lua", "markdown", "mdown", "md", "mysql", "sql", "nt", "ocaml", "pas", "pp", "lpr", "dpr", "pascal", "pl", "php", "pig", "plsql", "properties", "ini",
            "py", "r", "rpm", "rst", "rb", "rs", "scheme", "sh", "siv", "sieve", "st", "smarty", "rq", "stex", "tiddlywiki", "vb", "frm", "cs", "vbs", "vm", "v", "vh", "xml",
            "xhtml", "xquery", "xq", "xqy", "yml", "yaml", "z80"};

    protected static final String[] VIDEO_EXTENSIONS_ARRAY = {"3g2", "3gp", "avi", "m2v", "m2ts", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "ogg", "mts",
            "qt", "wmv"};
    protected static final String[] COMPRESSED_EXTENSIONS_ARRAY = {"zip", "rar", "gz", "tar", "7z", "arc", "ace", "tbz"};
    protected static final String[] INDESIGN_EXTENSIONS_ARRAY = {"indd", "indl", "indt", "indb", "inx", "idml", "pmd"};
    protected static final String[] OBJ_EXTENSIONS_ARRAY = {"obj", "3ds", "x3d"};
    protected static final String[] PHOTOSHOP_EXTENSIONS_ARRAY = {"psd", "psb"};
    protected static final String[] VECTOR_EXTENSIONS_ARRAY = {"eps", "svg"};

    protected static final ArrayList<String> IMAGE_EXTENSIONS = new ArrayList<String>();

    static {
        for (String ext : new String[] {"ai", "bmp", "dcm", "eps", "jpeg", "jpg", "png", "ps", "psd", "tif", "tiff", "svg", "gif"}){
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
        DEFAULT_ICON_RESORCE_MAP.put("ico", R.drawable.ic_box_browsesdk_icon);

        DEFAULT_ICON_RESORCE_MAP.put("boxnote", R.drawable.ic_box_browsesdk_box_note);
        DEFAULT_ICON_RESORCE_MAP.put("ai", R.drawable.ic_box_browsesdk_illustrator);
        DEFAULT_ICON_RESORCE_MAP.put("pdf", R.drawable.ic_box_browsesdk_pdf);

    }


    /**
     * Constructor.
     * 
     *
     * @param controller
     * @param cacheDir
     *            a file representing the directory to store thumbnail images.
     * @throws java.io.FileNotFoundException
     *             thrown if the directory given does not exist and cannot be created.
     */
    public ThumbnailManager(BrowseController controller, final File cacheDir) throws FileNotFoundException {
        mController = controller;

        // Ensure that parent cache directory is present
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        // Create box thumbnail directory.
        // This should be same as in preview to ensure preview can use thumbnails from here
        mThumbnailDirectory = new File(cacheDir, "BoxThumbnails");
        if (!mThumbnailDirectory.exists()) {
            mThumbnailDirectory.mkdir();
        }

        if (!mThumbnailDirectory.exists() || !mThumbnailDirectory.isDirectory()) {
            throw new FileNotFoundException();
        }
    }

    /**
     * Gets the default icon resource depending on what kind of boxItem is being viewed.
     * 
     * @param boxItem
     *            The box item to show to user.
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
     * @param boxItem
     * @return a File object where the thumbnail is saved to or should be saved to.
     */
    public File getThumbnailForBoxItem(final BoxItem boxItem) {
        if (boxItem instanceof BoxFile) {
            return getThumbnailForBoxFile((BoxFile) boxItem);
        }

        return null;
    }

    /**
     * Returns a file in a determinate location for the given boxItem.
     * 
     * @param boxFile
     *            box file.
     * @return a File object where the thumbnail is saved to or should be saved to.
     */
    public File getThumbnailForBoxFile(final BoxFile boxFile) {
        File file = new File(getThumbnailDirectory(), getCacheName(boxFile));
        try{
            file.createNewFile();
        } catch (IOException e){
            // Ignore errors in creating the file to store thumbnails to.
        }
        return file;
    }

    protected String getCacheName(BoxFile boxFile) {
        if (boxFile == null || SdkUtils.isBlank(boxFile.getId()) || SdkUtils.isBlank(boxFile.getSha1())) {
            throw new IllegalArgumentException("BoxFile argument must not be null and must also contain an id and sha1");
        }
        return String.format(Locale.ENGLISH, "%s_%s%s", boxFile.getId(), boxFile.getSha1(), THUMBNAIL_FILE_EXTENSION);
    }

    /**
     * @return the cacheDirectory of this thumbnail manager.
     */
    public File getThumbnailDirectory() {
        return mThumbnailDirectory;
    }

    /**
     * Convenience method to delete all files in the provided cache directory.
     */
    public void deleteFilesInCacheDirectory() {
        File[] files = mThumbnailDirectory.listFiles();
        if (files != null) {
            for (int index = 0; index < files.length; index++) {
                if (files[index].isFile()) {
                    files[index].delete();
                }
            }
        }
    }

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
     * @param item
     * @param targetImage
     */
    public void loadThumbnail(final BoxItem item, final ImageView targetImage) {
        if (item instanceof BoxFile && isThumbnailAvailable(item)) {
            // Cancel pending task upon recycle.
            BoxFutureTask task = mTargetToTask.remove(targetImage);
            if (task != null) {
                task.cancel(false);
            }

            // Placeholder should not be displayed (ie. set to null) if the file has already been fetched
            File thumbnailFile = getThumbnailForBoxFile((BoxFile) item);
            Bitmap placeHolderBitmap = (thumbnailFile.length() == 0 || !thumbnailFile.exists()) ?
                    BitmapFactory.decodeResource(targetImage.getResources(), getDefaultIconResource(item)) :
                    null;

            // Set the drawable to our loader drawable, which will show a placeholder before loading the thumbnail into the view
            BoxRequestsFile.DownloadThumbnail request = mController.getThumbnailRequest(item.getId(), thumbnailFile);
            LoaderDrawable loaderDrawable = LoaderDrawable.create(request, targetImage, placeHolderBitmap);
            targetImage.setImageDrawable(loaderDrawable);
            mTargetToTask.put(targetImage, loaderDrawable.getTask());
            getRequestExecutor().execute(loaderDrawable.getTask());
        } else {
            targetImage.setImageResource(getDefaultIconResource(item));
        }
    }

    /**
     * Executor that we will submit our set thumbnail tasks to.
     * 
     * @return executor
     */
    private ThreadPoolExecutor getRequestExecutor() {
        if (thumbnailRequestExecutor == null || thumbnailRequestExecutor.isShutdown()) {
            thumbnailRequestExecutor = new ThreadPoolExecutor(4, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return thumbnailRequestExecutor;
    }

}
