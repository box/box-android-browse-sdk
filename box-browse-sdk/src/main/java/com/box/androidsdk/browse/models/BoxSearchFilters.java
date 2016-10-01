package com.box.androidsdk.browse.models;

import java.io.Serializable;
import java.util.HashSet;

public class BoxSearchFilters implements Serializable {
    private static final long serialVersionUID = 1626794929492522854L;

    public enum ItemType {
        Audio,
        BoxNote,
        Document,
        Folder,
        Image,
        Pdf,
        Presentation,
        Spreadsheet,
        Video
    }

    public enum ItemModifiedDate {
        Any,
        PastDay,
        PastWeek,
        PastMonth,
        PastYear
    }

    public enum ItemSize {
        Any,
        lessThanOneMb,
        OneMbToFiveMb,
        FiveMbToTwentyFiveMb,
        TwentyFiveMbToHundredMb,
        HundredMbToOneGB
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
