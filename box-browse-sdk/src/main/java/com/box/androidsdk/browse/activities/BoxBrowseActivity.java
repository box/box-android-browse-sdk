package com.box.androidsdk.browse.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.fragments.BoxSearchFragment;
import com.box.androidsdk.browse.uidata.BoxSearchView;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class BoxBrowseActivity extends BoxThreadPoolExecutorActivity implements BoxBrowseFolderFragment.OnFragmentInteractionListener, BoxSearchView.OnBoxSearchListener {

    protected static final String EXTRA_SHOULD_SEARCH_ALL = "extraShouldSearchAll";

    protected static final String TAG = BoxBrowseActivity.class.getName();
    private static final String OUT_BROWSE_FRAGMENT = "outBrowseFragment";

    private static final ConcurrentLinkedQueue<BoxResponse> RESPONSE_QUEUE = new ConcurrentLinkedQueue<BoxResponse>();
    private static ThreadPoolExecutor mApiExecutor;
    private MenuItem mSearchViewMenuItem;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public ThreadPoolExecutor getApiExecutor(Application application) {
        if (mApiExecutor == null) {
            mApiExecutor = BoxThreadPoolExecutorActivity.createTaskMessagingExecutor(application, getResponseQueue());
        }
        return mApiExecutor;
    }

    @Override
    public Queue<BoxResponse> getResponseQueue() {
        return RESPONSE_QUEUE;
    }

    @Override
    protected void handleBoxResponse(BoxResponse response) {

    }

    protected BoxFolder getCurrentFolder() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (fragment instanceof BoxBrowseFolderFragment) {
            return ((BoxBrowseFolderFragment) fragment).getFolder();
        }
        return (BoxFolder) mItem;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.box_browsesdk_action_search) {
            // Launch search experience
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected BoxBrowseFragment getTopBrowseFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment frag = fragmentManager.findFragmentById(R.id.box_browsesdk_fragment_container);
        return frag instanceof BoxBrowseFragment ? (BoxBrowseFragment) frag : null;
    }

    protected void handleBoxFolderClicked(final BoxFolder boxFolder) {
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        // All fragments will always navigate into folders
        BoxBrowseFolderFragment browseFolderFragment = createBrowseFolderFragment(boxFolder, mSession);
        trans.replace(R.id.box_browsesdk_fragment_container, browseFolderFragment);
        if (getSupportFragmentManager().getBackStackEntryCount() > 0 || getSupportFragmentManager().getFragments() != null) {
            trans.addToBackStack(BoxBrowseFragment.TAG);
        }
        trans.commit();
    }

    @Override
    public void onBoxItemSelected(BoxItem boxItem) {
        clearSearch();
        if (boxItem instanceof BoxFolder) {
            handleBoxFolderClicked((BoxFolder) boxItem);
        }
    }

    /**
     * Creates a {@link BoxBrowseFolderFragment} that will be used in the activity to display
     * BoxItems. For a more customized experience, a custom implementation of the fragment can
     * be provided here.
     *
     * @param folder the folder that will be browsed
     * @param session the session that will be used for browsing
     * @return Browsing fragment that will be used to show the BoxItems
     */
    protected BoxBrowseFolderFragment createBrowseFolderFragment(final BoxItem folder, final BoxSession session) {
        return BoxBrowseFolderFragment.newInstance((BoxFolder) folder, mSession);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mSearchViewMenuItem = menu.findItem(R.id.box_browsesdk_action_search);
        BoxSearchView searchView = (BoxSearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);
        searchView.setSession(mSession);
        searchView.setOnBoxSearchListener(this);

        return true;
    }

    @Override
    public BoxRequestsSearch.Search onSearchRequested(BoxRequestsSearch.Search searchRequest) {
        if (!getIntent().getBooleanExtra(EXTRA_SHOULD_SEARCH_ALL, false)) {
            // if not specified by default search will only search the currently displayed folder.
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
            if (fragment instanceof BoxBrowseFolderFragment) {
                searchRequest.limitAncestorFolderIds(new String[]{((BoxBrowseFolderFragment) fragment).getFolder().getId()});
            } else {
                searchRequest.limitAncestorFolderIds(new String[]{mItem.getId()});
            }
        }
        return searchRequest;
    }

    private void clearSearch() {
        if (mSearchViewMenuItem == null) {
            return;
        }
        BoxSearchView searchView = (BoxSearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);
        searchView.onActionViewCollapsed();
    }

    @Override
    public void onMoreResultsRequested(BoxRequestsSearch.Search searchRequest) {
        clearSearch();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (fragment instanceof BoxSearchFragment) {
            ((BoxSearchFragment) fragment).search(onSearchRequested(searchRequest));
        } else {
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

            // All fragments will always navigate into folders
            BoxSearchFragment searchFragment = BoxSearchFragment.newInstance(mSession, onSearchRequested(searchRequest));
            trans.replace(R.id.box_browsesdk_fragment_container, searchFragment)
                    .addToBackStack(BoxBrowseFragment.TAG)
                    .commit();
        }
    }

    /**
     * Create a builder object that can be used to construct an intent to launch an instance of this activity.
     */
    protected static abstract class IntentBuilder<R> {
        final BoxSession mSession;
        final Context mContext;
        BoxFolder mFolder;
        boolean mShouldSearchAll = false;


        /**
         * Create an new Intent Builder designed to create an intent to launch a child of BoxBrowseActivity.
         *
         * @param context current context.
         * @param session an authenticated session.
         */
        public IntentBuilder(final Context context, final BoxSession session) {
            super();
            mContext = context;
            mSession = session;
            if (context == null)
                throw new IllegalArgumentException("A valid context must be provided to browse");
            if (session == null || session.getUser() == null || SdkUtils.isBlank(session.getUser().getId()))
                throw new IllegalArgumentException("A valid user must be provided to browse");

        }

        /**
         * @param folder folder to start browsing in.
         * @return an IntentBuilder which can create an instance of this class.
         */
        public R setStartingFolder(final BoxFolder folder) {
            mFolder = folder;
            if (folder == null || SdkUtils.isBlank(folder.getId()))
                throw new IllegalArgumentException("A valid folder must be provided");
            return (R) this;
        }

        /**
         * @param searchAll true if searching should search entire account, false if searching should only search current folder. False by default.
         * @return an IntentBuilder which can create an instance of this class.
         */
        public R setShouldSearchAll(final boolean searchAll) {
            mShouldSearchAll = searchAll;
            return (R) this;
        }

        /**
         * @param intent intent to add extras from this builder to.
         */
        protected void addExtras(final Intent intent) {
            intent.putExtra(EXTRA_SHOULD_SEARCH_ALL, mShouldSearchAll);
        }

        protected abstract Intent createLaunchIntent();

        /**
         * Create an intent to launch an instance of this activity.
         *
         * @return an intent to launch an instance of this activity.
         */
        public Intent createIntent() {
            Intent intent = createLaunchIntent();
            addExtras(intent);
            return intent;
        }
    }
}
