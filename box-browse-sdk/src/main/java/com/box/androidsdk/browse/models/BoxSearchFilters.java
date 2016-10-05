package com.box.androidsdk.browse.models;

import android.content.Context;

import com.box.androidsdk.browse.R;

import java.io.Serializable;
import java.util.HashSet;

public class BoxSearchFilters implements Serializable {
    private static final long serialVersionUID = 1626794929492522854L;

    public enum ItemType {
        Audio(R.id.audioFileTypeContainer, R.drawable.ic_box_browsesdk_audio, R.string.search_filter_file_type_audio),
        BoxNote(R.id.boxnoteFileTypeContainer, R.drawable.ic_box_browsesdk_box_note, R.string.search_filter_file_type_boxnote),
        Document(R.id.documentFileTypeContainer, R.drawable.ic_box_browsesdk_doc, R.string.search_filter_file_type_document),
        Folder(R.id.folderFileTypeContainer, R.drawable.ic_box_browsesdk_folder_shared, R.string.search_filter_file_type_folder),
        Image(R.id.imageFileTypeContainer, R.drawable.ic_box_browsesdk_image, R.string.search_filter_file_type_image),
        Pdf(R.id.pdfFileTypeContainer, R.drawable.ic_box_browsesdk_pdf, R.string.search_filter_file_type_pdf),
        Presentation(R.id.presentationFileTypeContainer, R.drawable.ic_box_browsesdk_presentation, R.string.search_filter_file_type_presentation),
        Spreadsheet(R.id.spreadsheetFileTypeContainer, R.drawable.ic_box_browsesdk_spreadsheet, R.string.search_filter_file_type_spreadsheet),
        Video(R.id.videoFileTypeContainer, R.drawable.ic_box_browsesdk_movie, R.string.search_filter_file_type_video);

        int mContainerId;
        int mStringId;
        int mDrawableId;

        ItemType(int containerId, int drawableId, int stringId) {
            mContainerId = containerId;
            mDrawableId = drawableId;
            mStringId = stringId;
        }

        public int getContainerId() {
            return mContainerId;
        }

        public int getDrawableId() {
            return mDrawableId;
        }

        public int getStringId() {
            return mStringId;
        }

        public String getString(Context context) {
            return context.getResources().getString(mStringId);
        }
    }

    public enum ItemModifiedDate {
        Any(R.id.dateModifiedContainerAnyTime, R.string.any_time),
        PastDay(R.id.dateModifiedContainerPastDay, R.string.past_day),
        PastWeek(R.id.dateModifiedContainerPastWeek, R.string.past_week),
        PastMonth(R.id.dateModifiedContainerPastMonth, R.string.past_month),
        PastYear(R.id.dateModifiedContainerPastYear, R.string.past_year);

        int mContainerId;
        int mStringId;

        ItemModifiedDate(int containerId, int stringId) {
            mContainerId = containerId;
            mStringId = stringId;
        }

        public int getContainerId() {
            return mContainerId;
        }

        public int getStringId() {
            return mStringId;
        }

        public String getString(Context context) {
            return context.getResources().getString(mStringId);
        }

    }

    public enum ItemSize {
        Any (R.id.itemSizeContainerAny, R.string.item_size_any),
        lessThanOneMb(R.id.itemSizeContainerLessThanOne, R.string.item_size_0_to_1),
        OneMbToFiveMb(R.id.itemSizeContainerOneToFive, R.string.item_size_1_to_5),
        FiveMbToTwentyFiveMb(R.id.itemSizeContainerFiveToTwentyFive, R.string.item_size_5_to_25),
        TwentyFiveMbToHundredMb(R.id.itemSizeContainerTwentyFiveToOneHundred, R.string.item_size_25_to_100),
        HundredMbToOneGB(R.id.itemSizeContainerOneHundredToOneThousand, R.string.item_size_100_to_1000);

        int mContainerId;
        int mStringId;

        ItemSize(int containerId, int stringId) {
            mContainerId = containerId;
            mStringId = stringId;
        }

        public int getContainerId() {
            return mContainerId;
        }

        public int getStringId() {
            return mStringId;
        }

        public String getString(Context context) {
            return context.getResources().getString(mStringId);
        }
    }

    public HashSet<ItemType> mItemTypes;
    public ItemModifiedDate mItemModifiedDate;
    public ItemSize mItemSize;

    public BoxSearchFilters() {
        mItemTypes = new HashSet<ItemType>();
        mItemModifiedDate = ItemModifiedDate.Any;
        mItemSize = ItemSize.Any;
    }

    public void addItemType(ItemType type) {
        if (!mItemTypes.contains(type)) {
            mItemTypes.add(type);
        }
    }

    public void removeItemType(ItemType type) {
        if (mItemTypes.contains(type)) {
            mItemTypes.remove(type);
        }
    }

    public boolean containsType(ItemType type) {
        return mItemTypes.contains(type);
    }

    public void setItemModifiedDate(ItemModifiedDate modifiedDate) {
        mItemModifiedDate = modifiedDate;
    }

    public void setItemSize(ItemSize itemSize) {
        mItemSize = itemSize;
    }

    public void clearFilters() {
        mItemTypes.clear();
        mItemModifiedDate = ItemModifiedDate.Any;
        mItemSize = ItemSize.Any;
    }

    public boolean anyFiltersSet() {
        return mItemTypes.size() > 0 ||
                mItemModifiedDate != ItemModifiedDate.Any ||
                mItemSize != ItemSize.Any;
    }

}
