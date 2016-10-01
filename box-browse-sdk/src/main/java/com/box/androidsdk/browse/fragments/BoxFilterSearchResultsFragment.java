package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.models.BoxSearchFilters;

import java.util.HashMap;

public class BoxFilterSearchResultsFragment extends Fragment {

    private static String EXTRA_FILTERS = "extraFilters";
    private BoxSearchFilters mFilters;
    private HashMap<BoxSearchFilters.ItemType, FileTypeData> mFileTypeMap;
    private boolean mDateModifiedExpanded;
    private boolean mItemSizeExpanded;

    public BoxFilterSearchResultsFragment() {
        // Required empty public constructor
    }


    public static BoxFilterSearchResultsFragment newInstance(BoxSearchFilters filters) {
        BoxFilterSearchResultsFragment fragment = new BoxFilterSearchResultsFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FILTERS, filters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFilters = (BoxSearchFilters) getArguments().getSerializable(EXTRA_FILTERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_box_filter_search_results, container, false);

        mDateModifiedExpanded = false;
        mItemSizeExpanded = false;
        setup(view);

        return view;
    }

    public BoxSearchFilters getCurrentFilters() {
        return mFilters;
    }

    private void setup(View view) {
        setupButtons(view);
        setupFileTypes(view);
        setupDateModified(view);
        setupSizeRange(view);
    }

    private void setupSizeRange(View view) {
        setupSizeRange(view, BoxSearchFilters.ItemSize.Any, R.id.itemSizeContainerAny, R.string.item_size_any);
        setupSizeRange(view, BoxSearchFilters.ItemSize.lessThanOneMb, R.id.itemSizeContainerLessThanOne, R.string.item_size_0_to_1);
        setupSizeRange(view, BoxSearchFilters.ItemSize.OneMbToFiveMb, R.id.itemSizeContainerOneToFive, R.string.item_size_1_to_5);
        setupSizeRange(view, BoxSearchFilters.ItemSize.FiveMbToTwentyFiveMb, R.id.itemSizeContainerFiveToTwentyFive, R.string.item_size_5_to_25);
        setupSizeRange(view, BoxSearchFilters.ItemSize.TwentyFiveMbToHundredMb, R.id.itemSizeContainerTwentyFiveToOneHundred, R.string.item_size_25_to_100);
        setupSizeRange(view, BoxSearchFilters.ItemSize.HundredMbToOneGB, R.id.itemSizeContainerOneHundredToOneThousand, R.string.item_size_100_to_1000);
    }

    private void setupSizeRange(final View view, final BoxSearchFilters.ItemSize itemSize, int containerViewId, int textResourceId) {
        final View container = view.findViewById(containerViewId);
        final TextView textView = (TextView) container.findViewById(R.id.text);
        final ImageView selectedImage = (ImageView) container.findViewById(R.id.selected);
        final ImageView expandImage = (ImageView) container.findViewById(R.id.expand);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemSizeExpanded = !mItemSizeExpanded;
                mFilters.setItemSize(itemSize);
                setupSizeRange(view);
                enableDisableClearButton(view);
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

    private void setupDateModified(View view) {
        setupDateModified(view, BoxSearchFilters.ItemModifiedDate.Any, R.id.dateModifiedContainerAnyTime, R.string.any_time);
        setupDateModified(view, BoxSearchFilters.ItemModifiedDate.PastDay, R.id.dateModifiedContainerPastDay, R.string.past_day);
        setupDateModified(view, BoxSearchFilters.ItemModifiedDate.PastWeek, R.id.dateModifiedContainerPastWeek, R.string.past_week);
        setupDateModified(view, BoxSearchFilters.ItemModifiedDate.PastMonth, R.id.dateModifiedContainerPastMonth, R.string.past_month);
        setupDateModified(view, BoxSearchFilters.ItemModifiedDate.PastYear, R.id.dateModifiedContainerPastYear, R.string.past_year);
    }

    private void setupDateModified(final View view, final BoxSearchFilters.ItemModifiedDate dateModified, int containerViewId, int textResourceId) {
        final View container = view.findViewById(containerViewId);
        final TextView textView = (TextView) container.findViewById(R.id.text);
        final ImageView selectedImage = (ImageView) container.findViewById(R.id.selected);
        final ImageView expandImage = (ImageView) container.findViewById(R.id.expand);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateModifiedExpanded = !mDateModifiedExpanded;
                mFilters.setItemModifiedDate(dateModified);
                setupDateModified(view);
                enableDisableClearButton(view);
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

    private void setupFileTypes(final View view) {
        mFileTypeMap = new HashMap<BoxSearchFilters.ItemType, FileTypeData>();

        setupFileType(view, BoxSearchFilters.ItemType.Audio, R.id.audioFileTypeContainer, R.drawable.ic_box_browsesdk_audio, R.string.search_filter_file_type_audio);
        setupFileType(view, BoxSearchFilters.ItemType.BoxNote, R.id.boxnoteFileTypeContainer, R.drawable.ic_box_browsesdk_box_note, R.string.search_filter_file_type_boxnote);
        setupFileType(view, BoxSearchFilters.ItemType.Document, R.id.documentFileTypeContainer, R.drawable.ic_box_browsesdk_doc, R.string.search_filter_file_type_document);
        setupFileType(view, BoxSearchFilters.ItemType.Folder, R.id.folderFileTypeContainer, R.drawable.ic_box_browsesdk_folder_shared, R.string.search_filter_file_type_folder);
        setupFileType(view, BoxSearchFilters.ItemType.Image, R.id.imageFileTypeContainer, R.drawable.ic_box_browsesdk_image, R.string.search_filter_file_type_image);
        setupFileType(view, BoxSearchFilters.ItemType.Pdf, R.id.pdfFileTypeContainer, R.drawable.ic_box_browsesdk_pdf, R.string.search_filter_file_type_pdf);
        setupFileType(view, BoxSearchFilters.ItemType.Presentation, R.id.presentationFileTypeContainer, R.drawable.ic_box_browsesdk_presentation, R.string.search_filter_file_type_presentation);
        setupFileType(view, BoxSearchFilters.ItemType.Spreadsheet, R.id.spreadsheetFileTypeContainer, R.drawable.ic_box_browsesdk_spreadsheet, R.string.search_filter_file_type_spreadsheet);
        setupFileType(view, BoxSearchFilters.ItemType.Video, R.id.videoFileTypeContainer, R.drawable.ic_box_browsesdk_movie, R.string.search_filter_file_type_video);

        final TextView seeMoreTextView = (TextView) view.findViewById(R.id.seeMoreFileType);
        seeMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seeMoreTextView.setVisibility(View.GONE);
                view.findViewById(R.id.hiddenFileTypes).setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupFileType(final View view, final BoxSearchFilters.ItemType type, int containerId, int icon, int text) {
        final RelativeLayout container = (RelativeLayout)view.findViewById(containerId);
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

                enableDisableClearButton(view);
            }
        });
    }

    private void setupButtons(final View view) {
        Button applyButton = (Button) view.findViewById(R.id.apply_button);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // finish activity here with result
                Intent intent = new Intent();
                intent.putExtra(BoxSearchFragment.EXTRA_SEARCH_FILTERS, mFilters);
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
            }
        });

        Button clearButton = (Button) view.findViewById(R.id.clear_filters_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear filters here
                mFilters.clearFilters();
                setup(view);
            }
        });
        enableDisableClearButton(view);
    }

    private void enableDisableClearButton(View view) {
        Button clearButton = (Button) view.findViewById(R.id.clear_filters_button);
        clearButton.setEnabled(mFilters.anyFiltersSet());
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
