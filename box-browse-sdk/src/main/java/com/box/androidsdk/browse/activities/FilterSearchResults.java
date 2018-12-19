package com.box.androidsdk.browse.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxFilterSearchResultsFragment;
import com.box.androidsdk.browse.models.BoxSearchFilters;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * The type Filter search results.
 */
public class FilterSearchResults extends AppCompatActivity {
    private static String EXTRA_FILTERS = "extraFilters";
    private BoxSearchFilters mFilters;
    private BoxFilterSearchResultsFragment mFragment;


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

        mFragment = (BoxFilterSearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (mFragment == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            mFragment = BoxFilterSearchResultsFragment.newInstance(mFilters);
            ft.add(R.id.fragmentContainer, mFragment);
            ft.commit();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (mFragment != null) {
            mFilters = mFragment.getCurrentFilters();
        }

        savedInstanceState.putSerializable(EXTRA_FILTERS, mFilters);
        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * New filter search results intent intent.
     *
     * @param context the context
     * @param filters current selected filters
     * @return the intent
     */
    public static Intent newFilterSearchResultsIntent(final Context context, BoxSearchFilters filters) {
        Intent intent = new Intent(context, FilterSearchResults.class);
        intent.putExtra(EXTRA_FILTERS, filters == null? new BoxSearchFilters(): filters);
        return intent;
    }

}

