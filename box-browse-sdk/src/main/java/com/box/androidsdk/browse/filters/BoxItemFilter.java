package com.box.androidsdk.browse.filters;


import com.box.androidsdk.content.models.BoxItem;

/**
 * An interface for filtering box items which are relevant for a given fragment
 *
 */

public interface BoxItemFilter {

     /**
     *
     * @param item
     * @return true if the item with given name should be returned by this fragment, false otherwise.
     */
    public boolean accept(final BoxItem item);
}
