package com.box.androidsdk.browse.filters;


import com.box.androidsdk.content.models.BoxItem;

/**
 * An interface for filtering box items which are relevant for a given fragment
 */
public interface BoxItemFilter {

    /**
     * Accept boolean.
     *
     * @param item a file, folder, or bookmark
     * @return return true if the item should be shown, false otherwise.
     */
    boolean accept(final BoxItem item);

    /**
     * Is enabled boolean.
     *
     * @param item a file, folder, or bookmark
     * @return return true if the item if shown should be enabled, false otherwise.
     */
    boolean isEnabled(final BoxItem item);

}
