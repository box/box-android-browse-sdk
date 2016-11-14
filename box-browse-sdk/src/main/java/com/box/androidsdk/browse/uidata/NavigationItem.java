package com.box.androidsdk.browse.uidata;


/**
 * This class will contain an item to display in navigation spinner..
 */
public final class NavigationItem {

    private String mName;
    private String mFolderId;

    /**
     * Instantiates a new Navigation item.
     *
     * @param name the name
     * @param id   the id
     */
    public NavigationItem(final String name, final String id) {
        mName = name;
        mFolderId = id;
    }

    public String toString() {
        return mName;
    }

    /**
     * Gets folder id.
     *
     * @return the folder id
     */
    public String getFolderId() {
        return mFolderId;
    }
}
