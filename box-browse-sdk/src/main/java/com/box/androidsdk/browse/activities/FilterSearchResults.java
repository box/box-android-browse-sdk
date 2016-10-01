package com.box.androidsdk.browse.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxSearchFragment;
import com.box.androidsdk.browse.models.BoxSearchFilters;

import java.util.HashMap;

public class FilterSearchResults extends AppCompatActivity {

    private static String EXTRA_FILTERS = "extraFilters";
    private BoxSearchFilters mFilters;
    private HashMap<BoxSearchFilters.ItemType, FileTypeData> mFileTypeMap;
    private boolean mDateModifiedExpanded;
    private boolean mItemSizeExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filter_search_results2);
        if (savedInstanceState != null) {
            mFilters = (BoxSearchFilters) savedInstanceState.getSerializable(EXTRA_FILTERS);
        }
        else {
            mFilters = (BoxSearchFilters) getIntent().getExtras().getSerializable(EXTRA_FILTERS);
        }

        mDateModifiedExpanded = false;
        mItemSizeExpanded = false;
        setup();
    }

    private void setup() {
        setupButtons();
        setupFileTypes();
        setupDateModified();
        setupSizeRange();
    }

    private void setupSizeRange() {
        setupSizeRange(BoxSearchFilters.ItemSize.Any, R.id.itemSizeContainerAny, R.string.item_size_any);
        setupSizeRange(BoxSearchFilters.ItemSize.lessThanOneMb, R.id.itemSizeContainerLessThanOne, R.string.item_size_0_to_1);
        setupSizeRange(BoxSearchFilters.ItemSize.OneMbToFiveMb, R.id.itemSizeContainerOneToFive, R.string.item_size_1_to_5);
        setupSizeRange(BoxSearchFilters.ItemSize.FiveMbToTwentyFiveMb, R.id.itemSizeContainerFiveToTwentyFive, R.string.item_size_5_to_25);
        setupSizeRange(BoxSearchFilters.ItemSize.TwentyFiveMbToHundredMb, R.id.itemSizeContainerTwentyFiveToOneHundred, R.string.item_size_25_to_100);
        setupSizeRange(BoxSearchFilters.ItemSize.HundredMbToOneGB, R.id.itemSizeContainerOneHundredToOneThousand, R.string.item_size_100_to_1000);
    }

    private void setupSizeRange(final BoxSearchFilters.ItemSize itemSize, int containerViewId, int textResourceId) {
        final View container = findViewById(containerViewId);
        final TextView textView = (TextView) container.findViewById(R.id.text);
        final ImageView selectedImage = (ImageView) container.findViewById(R.id.selected);
        final ImageView expandImage = (ImageView) container.findViewById(R.id.expand);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemSizeExpanded = !mItemSizeExpanded;
                mFilters.setItemSize(itemSize);
                setupSizeRange();
                enableDisableClearButton();
            }
        });

        textView.setText(getString(textResourceId));

        if (mFilters.mItemSize == itemSize) {
            // Selected item
            container.setVisibility(View.VISIBLE);
            selectedImage.setVisibility(mItemSizeExpanded? View.VISIBLE: View.GONE);
            expandImage.setVisibility(mItemSizeExpanded? View.GONE: View.VISIBLE);
            textView.setTextColor(getResources().getColor(R.color.primary));
        } else {
            // Unselected item
            container.setVisibility(mItemSizeExpanded? View.VISIBLE: View.GONE);
            selectedImage.setVisibility(View.GONE);
            expandImage.setVisibility(View.GONE);
            textView.setTextColor(getResources().getColor(R.color.black));
        }

    }

    private void setupDateModified() {
        setupDateModified(BoxSearchFilters.ItemModifiedDate.Any, R.id.dateModifiedContainerAnyTime, R.string.any_time);
        setupDateModified(BoxSearchFilters.ItemModifiedDate.PastDay, R.id.dateModifiedContainerPastDay, R.string.past_day);
        setupDateModified(BoxSearchFilters.ItemModifiedDate.PastWeek, R.id.dateModifiedContainerPastWeek, R.string.past_week);
        setupDateModified(BoxSearchFilters.ItemModifiedDate.PastMonth, R.id.dateModifiedContainerPastMonth, R.string.past_month);
        setupDateModified(BoxSearchFilters.ItemModifiedDate.PastYear, R.id.dateModifiedContainerPastYear, R.string.past_year);
    }

    private void setupDateModified(final BoxSearchFilters.ItemModifiedDate dateModified, int containerViewId, int textResourceId) {
        final View container = findViewById(containerViewId);
        final TextView textView = (TextView) container.findViewById(R.id.text);
        final ImageView selectedImage = (ImageView) container.findViewById(R.id.selected);
        final ImageView expandImage = (ImageView) container.findViewById(R.id.expand);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateModifiedExpanded = !mDateModifiedExpanded;
                mFilters.setItemModifiedDate(dateModified);
                setupDateModified();
                enableDisableClearButton();
            }
        });

        textView.setText(getString(textResourceId));

        if (mFilters.mItemModifiedDate == dateModified) {
            // Selected item
            container.setVisibility(View.VISIBLE);
            selectedImage.setVisibility(mDateModifiedExpanded? View.VISIBLE: View.GONE);
            expandImage.setVisibility(mDateModifiedExpanded? View.GONE: View.VISIBLE);
            textView.setTextColor(getResources().getColor(R.color.primary));
        } else {
            // Unselected item
            container.setVisibility(mDateModifiedExpanded? View.VISIBLE: View.GONE);
            selectedImage.setVisibility(View.GONE);
            expandImage.setVisibility(View.GONE);
            textView.setTextColor(getResources().getColor(R.color.black));
        }

    }

    private void setupFileTypes() {
        mFileTypeMap = new HashMap<BoxSearchFilters.ItemType, FileTypeData>();

        setupFileType(BoxSearchFilters.ItemType.Audio, R.id.audioFileTypeContainer, R.drawable.ic_box_browsesdk_audio, R.string.search_filter_file_type_audio);
        setupFileType(BoxSearchFilters.ItemType.BoxNote, R.id.boxnoteFileTypeContainer, R.drawable.ic_box_browsesdk_box_note, R.string.search_filter_file_type_boxnote);
        setupFileType(BoxSearchFilters.ItemType.Document, R.id.documentFileTypeContainer, R.drawable.ic_box_browsesdk_doc, R.string.search_filter_file_type_document);
        setupFileType(BoxSearchFilters.ItemType.Folder, R.id.folderFileTypeContainer, R.drawable.ic_box_browsesdk_folder_shared, R.string.search_filter_file_type_folder);
        setupFileType(BoxSearchFilters.ItemType.Image, R.id.imageFileTypeContainer, R.drawable.ic_box_browsesdk_image, R.string.search_filter_file_type_image);
        setupFileType(BoxSearchFilters.ItemType.Pdf, R.id.pdfFileTypeContainer, R.drawable.ic_box_browsesdk_pdf, R.string.search_filter_file_type_pdf);
        setupFileType(BoxSearchFilters.ItemType.Presentation, R.id.presentationFileTypeContainer, R.drawable.ic_box_browsesdk_presentation, R.string.search_filter_file_type_presentation);
        setupFileType(BoxSearchFilters.ItemType.Spreadsheet, R.id.spreadsheetFileTypeContainer, R.drawable.ic_box_browsesdk_spreadsheet, R.string.search_filter_file_type_spreadsheet);
        setupFileType(BoxSearchFilters.ItemType.Video, R.id.videoFileTypeContainer, R.drawable.ic_box_browsesdk_movie, R.string.search_filter_file_type_video);

        final TextView seeMoreTextView = (TextView) findViewById(R.id.seeMoreFileType);
        seeMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seeMoreTextView.setVisibility(View.GONE);
                findViewById(R.id.hiddenFileTypes).setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupFileType(final BoxSearchFilters.ItemType type, int containerId, int icon, int text) {
        final RelativeLayout container = (RelativeLayout)findViewById(containerId);
        final ImageView imageView = (ImageView) container.findViewById(R.id.icon);
        final TextView textView = (TextView) container.findViewById(R.id.text);
        final AppCompatCheckBox checkbox = (AppCompatCheckBox)container.findViewById(R.id.checkBox);

        imageView.setImageResource(icon);
        textView.setText(getString(text));

        FileTypeData fileTypeData = new FileTypeData(type, container, checkbox);
        mFileTypeMap.put(type, fileTypeData);

        // Initialize
        checkbox.setChecked(mFilters.containsType(type));

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkbox.setChecked(!checkbox.isChecked());
            }
        });

        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (type == BoxSearchFilters.ItemType.Folder) {
                        // UnCheck all files
                        for (BoxSearchFilters.ItemType itemType: BoxSearchFilters.ItemType.values()) {
                            if (itemType != BoxSearchFilters.ItemType.Folder && mFileTypeMap.get(itemType) != null) {
                                mFileTypeMap.get(itemType).mCheckBox.setChecked(false);
                            }
                        }
                    } else {
                        // UnCheck folder
                        mFileTypeMap.get(BoxSearchFilters.ItemType.Folder).mCheckBox.setChecked(false);
                    }

                    mFilters.addItemType(type);
                } else {
                    mFilters.removeItemType(type);
                }

                enableDisableClearButton();
            }
        });
    }

    private void setupButtons() {
        Button applyButton = (Button) findViewById(R.id.apply_button);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // finish activity here with result
                Intent intent = new Intent();
                intent.putExtra(BoxSearchFragment.EXTRA_SEARCH_FILTERS, mFilters);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        Button clearButton = (Button) findViewById(R.id.clear_filters_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear filters here
                mFilters.clearFilters();
                setup();
            }
        });
        enableDisableClearButton();
    }

    private void enableDisableClearButton() {
        Button clearButton = (Button) findViewById(R.id.clear_filters_button);
        clearButton.setEnabled(mFilters.anyFiltersSet());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(EXTRA_FILTERS, mFilters);
    }


    public static Intent newFilterSearchResultsIntent(final Context context, BoxSearchFilters filters) {
        Intent intent = new Intent(context, FilterSearchResults.class);
        intent.putExtra(EXTRA_FILTERS, filters == null? new BoxSearchFilters(): filters);
        return intent;
    }

    class FileTypeData {
        public BoxSearchFilters.ItemType mItemType;
        public RelativeLayout mContainer;
        public AppCompatCheckBox mCheckBox;

        public FileTypeData(final BoxSearchFilters.ItemType type, RelativeLayout container, AppCompatCheckBox checkbox) {
            mItemType = type;
            mContainer = container;
            mCheckBox = checkbox;
        }
    }

}

