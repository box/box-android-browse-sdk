package com.box.androidsdk.browse.uidata;

import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxRequest;


/**
 * This class will contain an item or a task that will be displayed in a list.
 */
public final class BoxListItem {
    public enum State {
        CREATED,
        SUBMITTED,
        ERROR
    }

    public static final int TYPE_BOX_FOLDER_ITEM = 0;
    public static final int TYPE_BOX_FILE_ITEM = 1;
    public static final int TYPE_FUTURE_TASK = 2;


    private BoxItem mBoxItem;
    private BoxRequest mRequest;
    private int mType;
    private String mIdentifier;
    private State mState = State.CREATED;
    private boolean mIsEnabled = true;

    /**
     * Constructor.
     *
     * @param boxItem box item that should be displayed to user.
     */
    public BoxListItem(BoxItem boxItem, final String identifier) {
        mBoxItem = boxItem;
        if (boxItem instanceof BoxFolder) {
            mType = TYPE_BOX_FOLDER_ITEM;
        } else {
            mType = TYPE_BOX_FILE_ITEM;
        }
        setIdentifier(identifier);
    }

    /**
     * Constructor.
     *
     * @param request task that should be performed if this item is gotten from the list.
     */
    public BoxListItem(BoxRequest request, final String identifier) {
        mRequest = request;
        mType = TYPE_FUTURE_TASK;
        setIdentifier(identifier);
    }

    /**
     * Gets the current state of the box list item
     *
     * @return
     */
    public State getState() {
        return mState;
    }

    /**
     * Sets the state of the box list item
     *
     * @param state
     */
    public void setState(State state) {
        mState = state;
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
     * Sets whether or not this list item should be enabled.
     *
     * @param isEnabled
     */
    public void setIsEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    /**
     * Returns the request associated with this item
     *
     * @return the task set for this item.
     */
    public BoxRequest getRequest() {
        return mRequest;
    }

    /**
     * Set a future task for the
     *
     * @param request
     */
    public void setRequest(BoxRequest request) {
        mRequest = request;
    }

    /**
     * @return the box item used in construction of this item.
     */
    public BoxItem getBoxItem() {
        return mBoxItem;
    }

    /**
     * @param boxItem set the given box item into this list item.
     */
    public void setBoxItem(final BoxItem boxItem) {
        mBoxItem = boxItem;
    }

    /**
     * @return the future task used in construction of this item.
     */
    public int getType() {
        return mType;
    }

    /**
     * Sets the type for the item
     */
    public void setType(int type) {
        mType = type;
    }

    /**
     * @return an identifier for the item (used as a performance enhancement).
     */
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * @param identifier sets the identifier (used as a performance enhancement).
     */
    private void setIdentifier(final String identifier) {
        mIdentifier = identifier;
    }

}
