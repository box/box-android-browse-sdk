package com.box.androidsdk.browse.uidata;

import android.content.Context;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

/**
 * This is a view used to show search results.
 */
public class BoxSearchView extends SearchView implements BoxSearchListAdapter.OnBoxSearchListener{

    private BoxSession mSession;
    private BoxApiSearch mSearchApi;
    private OnQueryTextListener mOnQueryTextListener;
    private OnBoxSearchListener mBoxSearchListener;
    private String mLastQuery = null;

   public BoxSearchView(final Context context){
       super(context);
       initSearchView(context);
   }



    public BoxSearchView(final Context context, final AttributeSet attrs){
        super(context, attrs);
        initSearchView(context);
    }

    private void initSearchView(final Context context){

        setSuggestionsAdapter(new BoxSearchListAdapter(context, R.layout.abc_list_menu_item_layout, 0, mSession));
        ((BoxSearchListAdapter)getSuggestionsAdapter()).setOnBoxSearchListener(this);

        if (mSession == null){
            // this widget cannot be used until a session has been set into it.
            this.setEnabled(false);
        }
        this.setOnSuggestionListener(new OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                if (mBoxSearchListener == null){
                    return false;
                }
                BoxSearchListAdapter.BoxSearchCursor cursor = (BoxSearchListAdapter.BoxSearchCursor)((BoxSearchListAdapter)getSuggestionsAdapter()).getItem(position);
                if (cursor.getType() == BoxSearchListAdapter.BoxSearchCursor.TYPE_NORMAL ){
                    mBoxSearchListener.onBoxItemSelected(cursor.getBoxItem());
                } else if (cursor.getType() == BoxSearchListAdapter.BoxSearchCursor.TYPE_ADDITIONAL_RESULT){
                    mBoxSearchListener.onMoreResultsRequested(mSearchApi.getSearchRequest(mLastQuery));
                }

                return false;
            }
        });
        this.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mLastQuery = query;
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mLastQuery = newText;
                return false;
            }
        });
    }

    @Override
    public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest) {
        if (mBoxSearchListener != null){
            return mBoxSearchListener.onSearchRequested(searchRequest);
        }
        return searchRequest;
    }

    public void setSession(final BoxSession session){
        mSession = session;
        mSearchApi = new BoxApiSearch(mSession);
        if (mSession != null){
            this.setEnabled(true);
        }
        ((BoxSearchListAdapter)getSuggestionsAdapter()).setSession(mSession);
    }


    private static final String EXTRA_ORIGINAL_PARCELABLE = "extraOriginalParcelable";
    private static final String EXTRA_USER_ID = "extraUserId";

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_ORIGINAL_PARCELABLE,parcelable);
        bundle.putString(EXTRA_USER_ID, mSession.getUserId());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            setSession(new BoxSession(getContext(), ((Bundle)state).getString(EXTRA_USER_ID) ));
            super.onRestoreInstanceState(((Bundle) state).getParcelable(EXTRA_ORIGINAL_PARCELABLE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    public class SearchItemCursor extends MatrixCursor {
        SearchItemCursor(String[] columnNames){
            super(columnNames);
        }
    }

    public void setOnBoxSearchListener(final OnBoxSearchListener onBoxSearchListener){
        mBoxSearchListener = onBoxSearchListener;
    }

    public static interface OnBoxSearchListener extends BoxSearchListAdapter.OnBoxSearchListener {

        /**
         * This is called if a user clicks on a file, folder, or bookmark from the suggestions.
         * @param boxItem The item the user clicks on from the suggestions.
         */
        public void onBoxItemSelected(final BoxItem boxItem);

        /**
         * This is called if a user clicks on the search icon, hits enter/search button on keyboard, or clicks on the more results item at the bottom of the suggestions.
         * @param searchRequest The request that this search was desired for.
         */
        public void onMoreResultsRequested(BoxRequestsSearch.Search searchRequest);

    }

}
