package com.box.androidsdk.browse.adapters;

import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;

/**
 * Fake item to be used as header for search results
 */
public class ResultsHeader extends BoxItem {
    /**
     * Instantiates a new Results header.
     *
     * @param parentFolder the folder in which search is performed
     */
    public ResultsHeader(BoxFolder parentFolder) {
        super(BoxFolder.createFromIdAndName(parentFolder.getId(), parentFolder.getName()).toJsonObject());
    }
}
