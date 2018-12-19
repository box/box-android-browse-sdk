package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.models.BoxSearchFilters;

import java.util.HashMap;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;

/**
 * This Fragment allows users to set filters on search parameters.
 */
public class BoxFilterSearchResultsFragment extends Fragment {
    private static String EXTRA_FILTERS = "extraFilters";
    private BoxSearchFilters mFilters;
    private HashMap<BoxSearchFilters.ItemType, FileTypeData> mFileTypeMap;
    private boolean mDateModifiedExpanded;
    private boolean mItemSizeExpanded;
    private ScrollView mScrollView;
    private LinearLayout mDateModifiedView;
    private LinearLayout mSizeView;


    /**
     * Instantiates a new Box filter search results fragment.
     */
    public BoxFilterSearchResultsFragment() {
        // Required empty public constructor
    }


    /**
     * New instance box filter search results fragment.
     *
     * @param filters current search filters
     * @return the box filter search results fragment
     */
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
        mScrollView = (ScrollView) view.findViewById(R.id.scrollView);
        mDateModifiedView = (LinearLayout) view.findViewById(R.id.dateModified);
        mSizeView = (LinearLayout) view.findViewById(R.id.size);

        mDateModifiedExpanded = false;
        mItemSizeExpanded = false;
        setup(view);

        return view;
    }

    /**
     * Gets current filters.
     *
     * @return the current filters
     */
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
        for (BoxSearchFilters.ItemSize itemSize: BoxSearchFilters.ItemSize.values()) {
            setupSizeRange(view, itemSize, itemSize.getContainerId(), itemSize.getStringId());
        }
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

                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.smoothScrollTo(0, mSizeView.getBottom());
                    }
                });
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
        for (BoxSearchFilters.ItemModifiedDate modifiedDate: BoxSearchFilters.ItemModifiedDate.values()) {
            setupDateModified(view, modifiedDate, modifiedDate.getContainerId(), modifiedDate.getStringId());
        }
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

                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.smoothScrollTo(0, mDateModifiedView.getBottom());
                    }
                });
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

        for (BoxSearchFilters.ItemType itemType: BoxSearchFilters.ItemType.values()) {
            setupFileType(view, itemType, itemType.getContainerId(), itemType.getDrawableId(), itemType.getStringId());
        }

        final TextView seeMoreTextView = (TextView) view.findViewById(R.id.seeMoreFileType);
        seeMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHiddenFileTypes(seeMoreTextView, view);
            }
        });

        if (mFilters.mItemTypes.size() > 0) {
            showHiddenFileTypes(seeMoreTextView, view);
        }
    }

    private void showHiddenFileTypes(TextView seeMoreTextView, View view) {
        seeMoreTextView.setVisibility(View.GONE);
        view.findViewById(R.id.hiddenFileTypes).setVisibility(View.VISIBLE);
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

    /**
     * The type File type data.
     */
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
