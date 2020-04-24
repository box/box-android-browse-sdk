package com.box.androidsdk.browse.models;

import android.content.Context;

import com.box.androidsdk.browse.R;

import java.io.Serializable;
import java.util.HashSet;

/**
 * BoxSearchFilters. This class stores the current set filters for a search
 */
public class BoxSearchFilters implements Serializable {
    private static final long serialVersionUID = 1626794929492522854L;

    /**
     * File types allowed for search request
     */
    public enum ItemType {
        /**
         * Audio item type.
         */
        Audio(R.id.audioFileTypeContainer, R.drawable.ic_box_browsesdk_audio, R.string.search_filter_file_type_audio),
        /**
         * Box note item type.
         */
        BoxNote(R.id.boxnoteFileTypeContainer, R.drawable.ic_box_browsesdk_box_note, R.string.search_filter_file_type_boxnote),
        /**
         * Document item type.
         */
        Document(R.id.documentFileTypeContainer, R.drawable.ic_box_browsesdk_doc, R.string.search_filter_file_type_document),
        /**
         * Autocad item type.
         */
        Autocad(R.id.autocadFileTypeContainer, R.drawable.ic_box_browsesdk_dwg, R.string.search_filter_file_type_autocad),
        /**
         * Folder item type.
         */
        Folder(R.id.folderFileTypeContainer, R.drawable.ic_box_browsesdk_folder_shared, R.string.search_filter_file_type_folder),
        /**
         * Image item type.
         */
        Image(R.id.imageFileTypeContainer, R.drawable.ic_box_browsesdk_image, R.string.search_filter_file_type_image),
        /**
         * Pdf item type.
         */
        Pdf(R.id.pdfFileTypeContainer, R.drawable.ic_box_browsesdk_pdf, R.string.search_filter_file_type_pdf),
        /**
         * Presentation item type.
         */
        Presentation(R.id.presentationFileTypeContainer, R.drawable.ic_box_browsesdk_presentation, R.string.search_filter_file_type_presentation),
        /**
         * Spreadsheet item type.
         */
        Spreadsheet(R.id.spreadsheetFileTypeContainer, R.drawable.ic_box_browsesdk_spreadsheet, R.string.search_filter_file_type_spreadsheet),
        /**
         * Video item type.
         */
        Video(R.id.videoFileTypeContainer, R.drawable.ic_box_browsesdk_movie, R.string.search_filter_file_type_video);

        /**
         * This is a resource id for the view that shows file type to the user.
         */
        int mContainerViewResId;
        /**
         * This is a resource id for the string that should be displayed for a file type
         */
        int mDisplayStringResId;
        /**
         * This is a resource id for the icon that should be displayed for the file type
         */
        int mIconResId;

        ItemType(int containerId, int drawableId, int stringId) {
            mContainerViewResId = containerId;
            mIconResId = drawableId;
            mDisplayStringResId = stringId;
        }

        /**
         * Gets container id.
         *
         * @return the container id
         */
        public int getContainerId() {
            return mContainerViewResId;
        }

        /**
         * Gets drawable id.
         *
         * @return the drawable id
         */
        public int getDrawableId() {
            return mIconResId;
        }

        /**
         * Gets string id.
         *
         * @return the string id
         */
        public int getStringId() {
            return mDisplayStringResId;
        }

        /**
         * Gets string.
         *
         * @param context the context
         * @return the string
         */
        public String getString(Context context) {
            return context.getResources().getString(mDisplayStringResId);
        }
    }

    /**
     * The enum Item modified date.
     */
    public enum ItemModifiedDate {
        /**
         * Any item modified date.
         */
        Any(R.id.dateModifiedContainerAnyTime, R.string.any_time),
        /**
         * Past day item modified date.
         */
        PastDay(R.id.dateModifiedContainerPastDay, R.string.past_day),
        /**
         * Past week item modified date.
         */
        PastWeek(R.id.dateModifiedContainerPastWeek, R.string.past_week),
        /**
         * Past month item modified date.
         */
        PastMonth(R.id.dateModifiedContainerPastMonth, R.string.past_month),
        /**
         * Past year item modified date.
         */
        PastYear(R.id.dateModifiedContainerPastYear, R.string.past_year);

        /**
         * This is a resource id for the view that shows the date modified to the user
         */
        int mContainerViewResId;
        /**
         * This is a resource id for the string that should be displayed for the date modified
         */
        int mDisplayStringResId;

        ItemModifiedDate(int containerId, int stringId) {
            mContainerViewResId = containerId;
            mDisplayStringResId = stringId;
        }

        /**
         * Gets container id.
         *
         * @return the container id
         */
        public int getContainerId() {
            return mContainerViewResId;
        }

        /**
         * Gets string id.
         *
         * @return the string id
         */
        public int getStringId() {
            return mDisplayStringResId;
        }

        /**
         * Gets string.
         *
         * @param context the context
         * @return the string
         */
        public String getString(Context context) {
            return context.getResources().getString(mDisplayStringResId);
        }

    }

    /**
     * The enum Item size.
     */
    public enum ItemSize {
        /**
         * Any item size.
         */
        Any (R.id.itemSizeContainerAny, R.string.item_size_any),
        /**
         * Less than one mb item size.
         */
        lessThanOneMb(R.id.itemSizeContainerLessThanOne, R.string.item_size_0_to_1),
        /**
         * One mb to five mb item size.
         */
        OneMbToFiveMb(R.id.itemSizeContainerOneToFive, R.string.item_size_1_to_5),
        /**
         * Five mb to twenty five mb item size.
         */
        FiveMbToTwentyFiveMb(R.id.itemSizeContainerFiveToTwentyFive, R.string.item_size_5_to_25),
        /**
         * Twenty five mb to hundred mb item size.
         */
        TwentyFiveMbToHundredMb(R.id.itemSizeContainerTwentyFiveToOneHundred, R.string.item_size_25_to_100),
        /**
         * Hundred mb to one gb item size.
         */
        HundredMbToOneGB(R.id.itemSizeContainerOneHundredToOneThousand, R.string.item_size_100_to_1000);

        /**
         * This is a resource id for the view that shows item size to the user
         */
        int mContainerViewResId;
        /**
         * This is a resource id for the string that should be displayed for an item size
         */
        int mDisplayStringResId;

        ItemSize(int containerId, int stringId) {
            mContainerViewResId = containerId;
            mDisplayStringResId = stringId;
        }

        /**
         * Gets container id.
         *
         * @return the container id
         */
        public int getContainerId() {
            return mContainerViewResId;
        }

        /**
         * Gets string id.
         *
         * @return the string id
         */
        public int getStringId() {
            return mDisplayStringResId;
        }

        /**
         * Gets string.
         *
         * @param context the context
         * @return the string
         */
        public String getString(Context context) {
            return context.getResources().getString(mDisplayStringResId);
        }
    }

    /**
     * The item types that has been selected by the user
     */
    public HashSet<ItemType> mItemTypes;
    /**
     * The item modified date that has been selected by the user
     */
    public ItemModifiedDate mItemModifiedDate;
    /**
     * The item size that has been selected by the user
     */
    public ItemSize mItemSize;

    /**
     * Instantiates a new Box search filters.
     */
    public BoxSearchFilters() {
        mItemTypes = new HashSet<ItemType>();
        mItemModifiedDate = ItemModifiedDate.Any;
        mItemSize = ItemSize.Any;
    }

    /**
     * Add item type to the mItemTypes
     *
     * @param type the type
     */
    public void addItemType(ItemType type) {
        if (!mItemTypes.contains(type)) {
            mItemTypes.add(type);
        }
    }

    /**
     * Remove item type from mItemTypes
     *
     * @param type the type
     */
    public void removeItemType(ItemType type) {
        if (mItemTypes.contains(type)) {
            mItemTypes.remove(type);
        }
    }

    /**
     * Contains type boolean. Check if an item type is selected by the user
     *
     * @param type the type
     * @return the boolean
     */
    public boolean containsType(ItemType type) {
        return mItemTypes.contains(type);
    }

    /**
     * Sets item modified date.
     *
     * @param modifiedDate the modified date
     */
    public void setItemModifiedDate(ItemModifiedDate modifiedDate) {
        mItemModifiedDate = modifiedDate;
    }

    /**
     * Sets item size.
     *
     * @param itemSize the item size
     */
    public void setItemSize(ItemSize itemSize) {
        mItemSize = itemSize;
    }

    /**
     * Clear filters.
     */
    public void clearFilters() {
        mItemTypes.clear();
        mItemModifiedDate = ItemModifiedDate.Any;
        mItemSize = ItemSize.Any;
    }

    /**
     * Any filters set boolean.
     *
     * @return the boolean
     */
    public boolean anyFiltersSet() {
        return mItemTypes.size() > 0 ||
                mItemModifiedDate != ItemModifiedDate.Any ||
                mItemSize != ItemSize.Any;
    }

}
