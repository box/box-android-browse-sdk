package com.box.androidsdk.browse.uidata;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.box.androidsdk.browse.R;

/**
 * Custom Search view to support new search design
 */
public class BoxSearchView extends SearchView {

    private OnBoxSearchListener mOnBoxSearchListener;

    // There is another property isIconified that represent the isExpanded state
    // but it is updated after code is executed in OnCloseListener call
    // Keeping a local property to ensure we know the state
    private boolean isExpanded;
    private int mClearIconDrawable;

    /**
     * Instantiates a new Box search view.
     *
     * @param context the context
     */
    public BoxSearchView(final Context context){
        super(context);
        initSearchView(context);
    }

    /**
     * Instantiates a new Box search view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public BoxSearchView(final Context context, final AttributeSet attrs){
        super(context, attrs);
        initSearchView(context);
        mClearIconDrawable = R.drawable.ic_clear_gray_24dp;
    }

    /**
     * Optional: Set your custom drawable for clear button. Clicking on the button will clear
     * the query text.
     * @param drawable Custom drawable
     */
    public void setCloseIconResource(int drawable) {
        mClearIconDrawable = drawable;
    }

    private void initSearchView(final Context context){
        isExpanded = false;

        LinearLayout searchPlate = (LinearLayout)findViewById(R.id.search_plate);
        searchPlate.setBackgroundColor(Color.TRANSPARENT);

        final ImageView searchCloseButton = (ImageView) searchPlate.findViewById(R.id.search_close_btn);
        searchCloseButton.setImageResource(mClearIconDrawable);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            EditText editText = ((EditText) findViewById(R.id.search_src_text));
            editText.setTextColor(Color.BLACK);
        }

        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_SEARCH| EditorInfo.IME_FLAG_NO_FULLSCREEN);
        setQueryHint(context.getString(R.string.box_browsesdk_search_hint));
        this.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (mOnBoxSearchListener != null) {
                    mOnBoxSearchListener.onQueryTextSubmit(query);
                }
                // Don't perform default action, return true
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mOnBoxSearchListener != null) {
                    mOnBoxSearchListener.onQueryTextChange(newText);
                }
                LinearLayout searchPlate = (LinearLayout)findViewById(R.id.search_plate);
                final ImageView searchCloseButton = (ImageView) searchPlate.findViewById(R.id.search_close_btn);

                if (!newText.isEmpty()) {
                    searchCloseButton.setImageResource(mClearIconDrawable);
                    searchCloseButton.setClickable(true);
                } else {
                    searchCloseButton.setImageResource(android.R.color.transparent);
                    searchCloseButton.setClickable(false);
                }
                // Don't perform default action, return true
                return true;
            }
        });

        this.setOnSearchClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isExpanded = true;
                if (mOnBoxSearchListener != null) {
                    mOnBoxSearchListener.onSearchExpanded();
                }
            }
        });

        this.setOnCloseListener(new OnCloseListener() {
            @Override
            public boolean onClose() {
                isExpanded = false;
                if (mOnBoxSearchListener != null) {
                    mOnBoxSearchListener.onSearchCollapsed();
                }
                return false;
            }
        });
    }

    /**
     * Is expanded boolean. returns true if the search view is expanded
     *
     * @return true if the view is expanded
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     * Sets search term in the search view
     *
     * @param query the query
     */
    public void setSearchTerm(String query) {
        setQuery(query, false);
    }

    private static final String EXTRA_ORIGINAL_PARCELABLE = "extraOriginalParcelable";

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_ORIGINAL_PARCELABLE,parcelable);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            super.onRestoreInstanceState(((Bundle) state).getParcelable(EXTRA_ORIGINAL_PARCELABLE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Set on box search listener.
     *
     * @param onBoxSearchListener the on box search listener
     */
    public void setOnBoxSearchListener(final OnBoxSearchListener onBoxSearchListener){
        mOnBoxSearchListener = onBoxSearchListener;
    }

    /**
     * The interface On box search listener.
     */
    public interface OnBoxSearchListener {

        /**
         * User clicked on search icon and expanded search
         */
        void onSearchExpanded();

        /**
         * User clicked on back and collapsed search
         */
        void onSearchCollapsed();

        /**
         * The text in search field has changed
         *
         * @param text the text
         */
        void onQueryTextChange(String text);

        /**
         * User pressed enter (or search button on keyboard) and submitted the search query
         *
         * @param text the text
         */
        void onQueryTextSubmit(String text);
    }
}
