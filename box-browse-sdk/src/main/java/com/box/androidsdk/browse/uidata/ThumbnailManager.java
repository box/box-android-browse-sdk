package com.box.androidsdk.browse.uidata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;


/**
 * 
 * This class manages thumbnails to display to users. This class does not do network calls.
 */
public class ThumbnailManager {

    /** The prefix added for thumbnails in this manager. */
    private static final String THUMBNAIL_FILE_PREFIX = "thumbnail_";

    /** The extension added for thumbnails in this manager. */
    private static final String THUMBNAIL_FILE_EXTENSION = ".boxthumbnail";

    /** The path where files in thumbnail should be stored. */
    private File mCacheDirectory;

    /** Handler on ui thread */
    private Handler mHandler;
    
    /** Executor that we will submit our set thumbnail tasks to. */
    private ThreadPoolExecutor thumbnailManagerExecutor;

    /**
     * Constructor.
     * 
     * @param cacheDirectoryPath
     *            the directory to store thumbnail images.
     * @throws java.io.FileNotFoundException
     *             thrown if the directory given does not exist and cannot be created.
     */
    public ThumbnailManager(final String cacheDirectoryPath) throws FileNotFoundException {
        this(new File(cacheDirectoryPath));
    }

    /**
     * Constructor.
     * 
     * @param cacheDirectory
     *            a file representing the directory to store thumbnail images.
     * @throws java.io.FileNotFoundException
     *             thrown if the directory given does not exist and cannot be created.
     */
    public ThumbnailManager(final File cacheDirectory) throws FileNotFoundException {
        mCacheDirectory = cacheDirectory;
        mCacheDirectory.mkdirs();
        if (!mCacheDirectory.exists() || !mCacheDirectory.isDirectory()) {
            throw new FileNotFoundException();
        }
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Sets the best known image for given item into the given view.
     * 
     * @param icon
     *            view to set image for.
     * @param boxItem
     *            the BoxItem used to set the view.
     */
    public void setThumbnailIntoView(final ImageView icon, final BoxItem boxItem) {
        final WeakReference<ImageView> iconHolder = new WeakReference<ImageView>(icon);
        final Resources res = icon.getResources();
        getThumbnailExecutor().execute(new Runnable() {

            public void run() {
                setThumbnail(iconHolder.get(), getDefaultIconResource(boxItem));
                Bitmap cachedIcon = getCachedIcon(boxItem);

                if (cachedIcon != null) {
                    setThumbnail(iconHolder.get(), new BitmapDrawable(res, cachedIcon));
                }
            }
        });

    }

    /**
     * Set the given image resource into given view on ui thread.
     * 
     * @param icon
     *            the ImageView to put drawable into.
     * @param imageRes
     *            the image resource to set into icon.
     */
    private void setThumbnail(final ImageView icon, final int imageRes) {
        mHandler.post(new Runnable() {

            public void run() {
                if (icon != null) {
                    icon.setImageResource(imageRes);
                }
            }
        });
    }

    /**
     * Set the given drawable into given view on ui thread.
     * 
     * @param icon
     *            the ImageView to put drawable into.
     * @param drawable
     *            the drawable to set into icon.
     */
    private void setThumbnail(final ImageView icon, final Drawable drawable) {
        mHandler.post(new Runnable() {

            public void run() {
                if (icon != null) {
                    icon.setImageDrawable(drawable);

                }
            }
        });

    }

    /**
     * Gets the default icon resource depending on what kind of boxItem is being viewed.
     * 
     * @param boxItem
     *            The box item to show to user.
     * @return an integer resource.
     */
    private int getDefaultIconResource(final BoxItem boxItem) {
        if (boxItem instanceof BoxFolder) {
            BoxFolder boxFolder = (BoxFolder) boxItem;
            if (boxFolder.getHasCollaborations() == Boolean.TRUE) {
                if (boxFolder.getIsExternallyOwned() == Boolean.TRUE) {
                    return R.drawable.ic_box_browsesdk_folder_external;
                }
                return R.drawable.ic_box_browsesdk_folder_shared;
            }
            return R.drawable.ic_box_browsesdk_folder;
        } else if (boxItem instanceof BoxBookmark) {
            return R.drawable.ic_box_browsesdk_weblink;
        } else {
            String name = boxItem.getName();
            if (name != null) {
                String ext = name.substring(name.lastIndexOf('.') + 1);
                if (ext.equals("aac")) {
                        return R.drawable.ic_box_browsesdk_aac;
                } else if (ext.equals("ai")) {
                        return R.drawable.ic_box_browsesdk_ai;
                } else if (ext.equals("aiff")) {
                        return R.drawable.ic_box_browsesdk_aiff;
                } else if (ext.equals("aspx")) {
                        return R.drawable.ic_box_browsesdk_aspx;
                } else if (ext.equals("avi")) {
                        return R.drawable.ic_box_browsesdk_avi;
                } else if (ext.equals("blank")) {
                        return R.drawable.ic_box_browsesdk_blank;
                } else if (ext.equals("bmp")) {
                        return R.drawable.ic_box_browsesdk_bmp;
                } else if (ext.equals("boxnote")) {
                        return R.drawable.ic_box_browsesdk_boxnote;
                } else if (ext.equals("c")) {
                        return R.drawable.ic_box_browsesdk_c;
                } else if (ext.equals("cpp")) {
                        return R.drawable.ic_box_browsesdk_cpp;
                } else if (ext.equals("css")) {
                        return R.drawable.ic_box_browsesdk_css;
                } else if (ext.equals("csv")) {
                        return R.drawable.ic_box_browsesdk_csv;
                } else if (ext.equals("db")) {
                        return R.drawable.ic_box_browsesdk_db;
                } else if (ext.equals("dcm")) {
                        return R.drawable.ic_box_browsesdk_dcm;
                } else if (ext.equals("doc")) {
                        return R.drawable.ic_box_browsesdk_doc;
                } else if (ext.equals("docx")) {
                        return R.drawable.ic_box_browsesdk_docx;
                } else if (ext.equals("dot")) {
                        return R.drawable.ic_box_browsesdk_dot;
                } else if (ext.equals("dotx")) {
                        return R.drawable.ic_box_browsesdk_dotx;
                } else if (ext.equals("eps")) {
                        return R.drawable.ic_box_browsesdk_eps;
                } else if (ext.equals("flv")) {
                        return R.drawable.ic_box_browsesdk_flv;
                } else if (ext.equals("gdoc")) {
                        return R.drawable.ic_box_browsesdk_gdoc;
                } else if (ext.equals("gdraw")) {
                        return R.drawable.ic_box_browsesdk_gdraw;
                } else if (ext.equals("generic")) {
                        return R.drawable.ic_box_browsesdk_generic;
                } else if (ext.equals("gif")) {
                        return R.drawable.ic_box_browsesdk_gif;
                } else if (ext.equals("gsheet")) {
                        return R.drawable.ic_box_browsesdk_gsheet;
                } else if (ext.equals("gslide")) {
                        return R.drawable.ic_box_browsesdk_gslide;
                } else if (ext.equals("htm")) {
                        return R.drawable.ic_box_browsesdk_htm;
                } else if (ext.equals("html")) {
                        return R.drawable.ic_box_browsesdk_html;
                } else if (ext.equals("indd")) {
                        return R.drawable.ic_box_browsesdk_indd;
                } else if (ext.equals("java")) {
                        return R.drawable.ic_box_browsesdk_java;
                } else if (ext.equals("jpeg")) {
                        return R.drawable.ic_box_browsesdk_jpeg;
                } else if (ext.equals("jpg")) {
                        return R.drawable.ic_box_browsesdk_jpg;
                } else if (ext.equals("js")) {
                        return R.drawable.ic_box_browsesdk_js;
                } else if (ext.equals("key")) {
                        return R.drawable.ic_box_browsesdk_key;
                } else if (ext.equals("link")) {
                        return R.drawable.ic_box_browsesdk_link;
                } else if (ext.equals("m3u")) {
                        return R.drawable.ic_box_browsesdk_m3u;
                } else if (ext.equals("m4a")) {
                        return R.drawable.ic_box_browsesdk_m4a;
                } else if (ext.equals("m4v")) {
                        return R.drawable.ic_box_browsesdk_m4v;
                } else if (ext.equals("markdown")) {
                        return R.drawable.ic_box_browsesdk_markdown;
                } else if (ext.equals("md")) {
                        return R.drawable.ic_box_browsesdk_md;
                } else if (ext.equals("mdown")) {
                        return R.drawable.ic_box_browsesdk_mdown;
                } else if (ext.equals("mid")) {
                        return R.drawable.ic_box_browsesdk_mid;
                } else if (ext.equals("mov")) {
                        return R.drawable.ic_box_browsesdk_mov;
                } else if (ext.equals("mp3")) {
                        return R.drawable.ic_box_browsesdk_mp3;
                } else if (ext.equals("mp4")) {
                        return R.drawable.ic_box_browsesdk_mp4;
                } else if (ext.equals("mpeg")) {
                        return R.drawable.ic_box_browsesdk_mpeg;
                } else if (ext.equals("mpg")) {
                        return R.drawable.ic_box_browsesdk_mpg;
                } else if (ext.equals("numbers")) {
                        return R.drawable.ic_box_browsesdk_numbers;
                } else if (ext.equals("obj")) {
                        return R.drawable.ic_box_browsesdk_obj;
                } else if (ext.equals("odp")) {
                        return R.drawable.ic_box_browsesdk_odp;
                } else if (ext.equals("ods")) {
                        return R.drawable.ic_box_browsesdk_ods;
                } else if (ext.equals("odt")) {
                        return R.drawable.ic_box_browsesdk_odt;
                } else if (ext.equals("otp")) {
                        return R.drawable.ic_box_browsesdk_otp;
                } else if (ext.equals("ots")) {
                        return R.drawable.ic_box_browsesdk_ots;
                } else if (ext.equals("ott")) {
                        return R.drawable.ic_box_browsesdk_ott;
                } else if (ext.equals("pages")) {
                        return R.drawable.ic_box_browsesdk_pages;
                } else if (ext.equals("pdf")) {
                        return R.drawable.ic_box_browsesdk_pdf;
                } else if (ext.equals("php")) {
                        return R.drawable.ic_box_browsesdk_php;
                } else if (ext.equals("png")) {
                        return R.drawable.ic_box_browsesdk_png;
                } else if (ext.equals("pot")) {
                        return R.drawable.ic_box_browsesdk_pot;
                } else if (ext.equals("potx")) {
                        return R.drawable.ic_box_browsesdk_potx;
                } else if (ext.equals("ppt")) {
                        return R.drawable.ic_box_browsesdk_ppt;
                } else if (ext.equals("pptx")) {
                        return R.drawable.ic_box_browsesdk_pptx;
                } else if (ext.equals("psd")) {
                        return R.drawable.ic_box_browsesdk_psd;
                } else if (ext.equals("qt")) {
                        return R.drawable.ic_box_browsesdk_qt;
                } else if (ext.equals("rar")) {
                        return R.drawable.ic_box_browsesdk_rar;
                } else if (ext.equals("rtf")) {
                        return R.drawable.ic_box_browsesdk_rtf;
                } else if (ext.equals("scala")) {
                        return R.drawable.ic_box_browsesdk_scala;
                } else if (ext.equals("sql")) {
                        return R.drawable.ic_box_browsesdk_sql;
                } else if (ext.equals("svg")) {
                        return R.drawable.ic_box_browsesdk_svg;
                } else if (ext.equals("tgz")) {
                        return R.drawable.ic_box_browsesdk_tgz;
                } else if (ext.equals("tiff")) {
                        return R.drawable.ic_box_browsesdk_tiff;
                } else if (ext.equals("txt")) {
                        return R.drawable.ic_box_browsesdk_txt;
                } else if (ext.equals("wav")) {
                        return R.drawable.ic_box_browsesdk_wav;
                } else if (ext.equals("webba")) {
                        return R.drawable.ic_box_browsesdk_webba;
                } else if (ext.equals("weblink")) {
                        return R.drawable.ic_box_browsesdk_weblink;
                } else if (ext.equals("wma")) {
                        return R.drawable.ic_box_browsesdk_wma;
                } else if (ext.equals("wmv")) {
                        return R.drawable.ic_box_browsesdk_wmv;
                } else if (ext.equals("wpl")) {
                        return R.drawable.ic_box_browsesdk_wpl;
                } else if (ext.equals("xhtml")) {
                        return R.drawable.ic_box_browsesdk_xhtml;
                } else if (ext.equals("xls")) {
                        return R.drawable.ic_box_browsesdk_xls;
                } else if (ext.equals("xlsx")) {
                        return R.drawable.ic_box_browsesdk_xlsx;
                } else if (ext.equals("xlt")) {
                        return R.drawable.ic_box_browsesdk_xlt;
                } else if (ext.equals("xltx")) {
                        return R.drawable.ic_box_browsesdk_xltx;
                } else if (ext.equals("xml")) {
                        return R.drawable.ic_box_browsesdk_xml;
                } else if (ext.equals("zip")) {
                        return R.drawable.ic_box_browsesdk_zip;
                }
            }
            return R.drawable.ic_box_browsesdk_blank;
        }
    }

    /**
     * Gets a bitmap for the item if one exists.
     * 
     * @param boxItem
     *            The box item to show to user.
     * @return an integer resource.
     */
    private Bitmap getCachedIcon(final BoxItem boxItem) {
        File file = getCachedFile(boxItem);
        if (file.exists() && file.isFile()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            return bitmap;
        }
        return null;
    }

    /**
     * Returns a file in a determinate location for the given boxItem.
     * 
     * @param boxItem
     *            the box item to generate file or get file for.
     * @return a File object where the icon is located or written to if non existent.
     */
    private File getCachedFile(final BoxItem boxItem) {
        return getThumbnailForFile(boxItem.getId());

    }

    /**
     * Returns a file in a determinate location for the given boxItem.
     * 
     * @param fileId
     *            The id of the box file.
     * @return a File object where the thumbnail is saved to or should be saved to.
     */
    public File getThumbnailForFile(final String fileId) {
        File file = new File(getCacheDirectory(), THUMBNAIL_FILE_PREFIX + fileId + THUMBNAIL_FILE_EXTENSION);
        try{
            file.createNewFile();
        } catch (IOException e){
            // Ignore errors in creating the file to store thumbnails to.
        }
        return file;
    }

    /**
     * @return the cacheDirectory of this thumbnail manager.
     */
    public File getCacheDirectory() {
        return mCacheDirectory;
    }

    /**
     * Convenience method to delete all files in the provided cache directory.
     */
    public void deleteFilesInCacheDirectory() {
        File[] files = mCacheDirectory.listFiles();
        if (files != null) {
            for (int index = 0; index < files.length; index++) {
                if (files[index].isFile()) {
                    files[index].delete();
                }
            }
        }
    }

  

    /**
     * Executor that we will submit our set thumbnail tasks to.
     * 
     * @return executor
     */
    private ThreadPoolExecutor getThumbnailExecutor() {
        if (thumbnailManagerExecutor == null || thumbnailManagerExecutor.isShutdown()) {
            thumbnailManagerExecutor = new ThreadPoolExecutor(4, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return thumbnailManagerExecutor;
    }

}
