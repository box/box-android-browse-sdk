package com.box.androidsdk.browse.filters;


import com.box.androidsdk.content.models.BoxItem;

/**
 * An interface for filtering box items which are relevant for a given fragment
 */
public interface BoxItemFilter {

    /**
     * Method which decides whether a file should be shown or not.
     *
     * @param item a file, folder, or bookmark
     * @return return true if the item should be shown, false otherwise.
     */
    boolean accept(final BoxItem item);

    /**
     * Method which decides whether file should be enabled or disabled.
     *
     * @param item a file, folder, or bookmark
     * @return return true if the item if shown should be enabled, false otherwise.
     */
    boolean isEnabled(final BoxItem item);

}
