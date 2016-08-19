package com.box.androidsdk.browse.uidata;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.box.androidsdk.browse.R;

/**
 * Custom Search view to support new search design
 */
public class BoxSearchView extends SearchView {

    private BoxCustomSearchListener mBoxCustomSearchListener;

    public BoxSearchView(final Context context){
        super(context);
        initSearchView(context);
    }

    public BoxSearchView(final Context context, final AttributeSet attrs){
        super(context, attrs);
        initSearchView(context);
    }

    private void initSearchView(final Context context){

        findViewById(R.id.search_plate).setBackgroundColor(Color.TRANSPARENT);
        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_SEARCH| EditorInfo.IME_FLAG_NO_FULLSCREEN);
        setQueryHint(context.getString(R.string.box_browsesdk_search_hint));
        this.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                mBoxCustomSearchListener.onQueryTextSubmit(query);

                // Don't perform default action, return true
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                mBoxCustomSearchListener.onQueryTextChange(newText);

                // Don't perform default action, return true
                return true;
            }
        });

        this.setOnSearchClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBoxCustomSearchListener.onSearchExpanded();
            }
        });

        this.setOnCloseListener(new OnCloseListener() {
            @Override
            public boolean onClose() {
                mBoxCustomSearchListener.onSearchCollapsed();
                return false;
            }
        });
    }

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

    public void setOnBoxSearchListener(final BoxCustomSearchListener boxCustomSearchListener){
        mBoxCustomSearchListener = boxCustomSearchListener;
    }

    public interface BoxCustomSearchListener {

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
         * @param text
         */
        void onQueryTextChange(String text);

        /**
         * User pressed enter (or search button on keyboard) and submitted the search query
         * @param text
         */
        void onQueryTextSubmit(String text);
    }
}
