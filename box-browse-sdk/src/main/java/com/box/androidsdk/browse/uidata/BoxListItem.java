package com.box.androidsdk.browse.uidata;

import android.content.Intent;

import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;

import java.util.concurrent.FutureTask;


/**
 * 
 * This class will contain an item or a task that will be displayed in a list.
 */
public final class BoxListItem {
    public static final int TYPE_BOX_FOLDER_ITEM = 0;
    public static final int TYPE_BOX_FILE_ITEM = 1;
    public static final int TYPE_FUTURE_TASK = 2;


    private BoxItem mBoxItem;
    private FutureTask<Intent> mTask;
    private int mType;
    private String mIdentifier;
    private boolean mIsError = false;
    private boolean mIsEnabled = true;

    /**
     * Constructor.
     * 
     * @param boxItem
     *            box item that should be displayed to user.
     */
    public BoxListItem(BoxItem boxItem, final String identifier) {
        mBoxItem = boxItem;
        if (boxItem instanceof BoxFolder) {
            mType = TYPE_BOX_FOLDER_ITEM;
        }
        else {
            mType = TYPE_BOX_FILE_ITEM;
        }
        setIdentifier(identifier);
    }

    /**
     * Constructor.
     * 
     * @param task
     *            task that should be performed if this item is gotten from the list.
     */
    public BoxListItem(FutureTask<Intent> task, final String identifier) {
        mTask = task;
        mType = TYPE_FUTURE_TASK;
        setIdentifier(identifier);
    }

    /**
     * Set a future task for the
     * 
     * @param task
     */
    public void setTask(FutureTask<Intent> task) {
        mTask = task;

    }

    /**
     * Sets the type for the item
     */
    public void setType(int type) {
        mType = type;
    }

    /**
     * Sets whether or not this list item represents an error
     *
     * @param isError
     */
    public void setIsError(boolean isError) {
        mIsError = isError;
    }

    /**
     * Gets whether or not this list item represents an error
     *
     * @return whether or not this item is an error state
     */
    public boolean getIsError() {
        return mIsError;
    }


    /**
     * Sets whether or not this list item should be enabled.
     *
     * @param isEnabled
     */
    public void setIsEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    /**
     * Gets whether or not this list item should be enabled
     *
     * @return whether or not this item is an error state
     */
    public boolean getIsEnabled() {
        return mIsEnabled;
    }


    /**
     * 
     * @return the task set for this item.
     */
    public FutureTask<Intent> getTask() {
        return mTask;

    }

    /**
     * 
     * @return the box item used in construction of this item.
     */
    public BoxItem getBoxItem() {
        return mBoxItem;
    }

    /**
     *
     * @param boxItem set the given box item into this list item.
     */
    public void setBoxItem(final BoxItem boxItem) {
        mBoxItem = boxItem;
    }


    /**
     * 
     * @return the future task used in construction of this item.
     */
    public int getType() {
        return mType;
    }

    /**
     * 
     * @return an identifier for the item (used as a performance enhancement).
     */
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * 
     * @param identifier
     *            sets the identifier (used as a performance enhancement).
     */
    private void setIdentifier(final String identifier) {
        mIdentifier = identifier;
    }

}
