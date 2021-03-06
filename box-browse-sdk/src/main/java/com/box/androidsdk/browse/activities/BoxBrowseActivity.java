package com.box.androidsdk.browse.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxRecentSearchAdapter;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.fragments.BoxSearchFragment;
import com.box.androidsdk.browse.fragments.OnUpdateListener;
import com.box.androidsdk.browse.service.BoxBrowseController;
import com.box.androidsdk.browse.service.BrowseController;
import com.box.androidsdk.browse.uidata.BoxSearchView;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Box browse activity
 * This is the base activity BoxFolderActivity and BoxFileActivity. It implements the common functionality like initiating search feature, handling navigation etc.
 */
public abstract class BoxBrowseActivity extends BoxThreadPoolExecutorActivity implements BoxBrowseFragment.OnItemClickListener, BoxSearchView.OnBoxSearchListener, FragmentManager.OnBackStackChangedListener {
    protected static final String TAG = BoxBrowseActivity.class.getName();

    private static final ConcurrentLinkedQueue<BoxResponse> RESPONSE_QUEUE = new ConcurrentLinkedQueue<BoxResponse>();
    private static final String RESTORE_SEARCH = "restoreSearch";
    private static final String SEARCH_QUERY = "searchQuery";
    private static ThreadPoolExecutor mApiExecutor;
    private MenuItem mSearchViewMenuItem;
    private BrowseController mController;
    private OnUpdateListener mUpdateListener;

    private BoxSearchView mSearchView;
    private ListView mRecentSearchesListView;
    protected ArrayList<String> mRecentSearches;
    protected BoxRecentSearchAdapter mRecentSearchesAdapter;
    private View mRecentSearchesHeader;
    private View mRecentSearchesFooter;

    private BoxFolder mCurrentBoxFolder;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mController = new BoxBrowseController(mSession, new BoxApiFile(mSession),
                new BoxApiFolder(mSession),
                new BoxApiSearch(mSession));

        if (savedInstanceState != null) {
            mCurrentBoxFolder = (BoxFolder) savedInstanceState.getSerializable(EXTRA_ITEM);
        } else if (getIntent() != null) {
            mCurrentBoxFolder = (BoxFolder) getIntent().getSerializableExtra(EXTRA_ITEM);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        super.onDestroy();
    }

    /**
     * Init recent searches.
     *
     * It initiates the views related to recent searches, like recent searches listview, header and footer of the list etc.
     */
    public void initRecentSearches() {
        mRecentSearchesHeader = getLayoutInflater().inflate(com.box.androidsdk.browse.R.layout.box_browsesdk_recent_searches_header, null);
        mRecentSearchesFooter = getLayoutInflater().inflate(com.box.androidsdk.browse.R.layout.box_browsesdk_recent_searches_footer, null);
        mRecentSearchesListView = (ListView) findViewById(R.id.recentSearchesListView);
        mRecentSearchesListView.addHeaderView(mRecentSearchesHeader);
        mRecentSearchesListView.addFooterView(mRecentSearchesFooter);
        mRecentSearchesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String query = mRecentSearchesAdapter.getItem(position - mRecentSearchesListView.getHeaderViewsCount());
                mSearchView.setSearchTerm(query);
            }
        });
        mRecentSearchesListView.setVisibility(View.GONE);
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

    /**
     * Gets current folder.
     *
     * @return the current folder
     */
    protected BoxFolder getCurrentFolder() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        BoxFolder curFolder = fragment instanceof BoxBrowseFolderFragment ?
                    ((BoxBrowseFolderFragment) fragment).getFolder() :
                    null;
        return curFolder;
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

    /**
     * Get top browse fragment
     * As user is navigating folders, BoxBrowseActivity will have a stack of browse fragment, each representing a folder.
     * This method returns the top browse fragment from the stack.
     *
     * @return the top browse fragment
     */
    protected BoxBrowseFragment getTopBrowseFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment frag = fragmentManager.findFragmentById(R.id.box_browsesdk_fragment_container);
        return frag instanceof BoxBrowseFragment ? (BoxBrowseFragment) frag : null;
    }

    /**
     * Handle box folder clicked.
     * This method is called when user clicks on a folder. It will create a new browse fragment,
     * with the folder that user has clicked and will push it on to the stack of browse fragments
     *
     * @param boxFolder folder on which the user has clicked
     */
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

    /**
     * Creates a {@link BoxBrowseFolderFragment} that will be used in the activity to display
     * BoxItems. For a more customized experience, a custom implementation of the fragment can
     * be provided here.
     *
     * @param folder  the folder that will be browsed
     * @param session the session that will be used for browsing
     * @return Browsing fragment that will be used to show the BoxItems
     */
    protected BoxBrowseFolderFragment createBrowseFolderFragment(final BoxItem folder, final BoxSession session) {
        final BoxBrowseFolderFragment fragment = new BoxBrowseFolderFragment.Builder((BoxFolder) folder, session).build();
        if (mUpdateListener == null) {
            mUpdateListener = new OnUpdateListener() {
                @Override
                public void onUpdate() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            setTitle(getCurrentFolder());
                        }
                    });
                }
            };
        }

        fragment.addOnUpdateListener(mUpdateListener);
        return fragment;
    }


    /**
     * Sets the title in the toolbar.
     * This method reads the folder name and sets it as the title in the toolbar.
     *
     * @param folder current folder, whose name should be used as a title
     */
    protected void setTitle(final BoxFolder folder) {
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null && folder != null) {
            actionbar.setTitle(folder.getId() == BoxConstants.ROOT_FOLDER_ID
                    ? getString(R.string.box_browsesdk_all_files) : folder.getName());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        mSearchViewMenuItem = menu.findItem(R.id.box_browsesdk_action_search);
        mSearchView = (BoxSearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);

        if (fragment instanceof BoxSearchFragment) {
            mSearchView.setIconified(false);
            mSearchView.setSearchTerm(((BoxSearchFragment)fragment).getSearchQuery());
        }

        enableDisableRecentView();
        mSearchView.setOnBoxSearchListener(this);

        return true;
    }


    private void enableDisableRecentView() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        boolean recentSearchesHidden = fragment instanceof BoxSearchFragment || !mSearchView.isExpanded();
        mRecentSearchesListView.setVisibility(recentSearchesHidden? View.GONE : View.VISIBLE);
        if (mRecentSearches == null || mRecentSearches.size() == 0) {
            mRecentSearchesHeader.setVisibility(View.GONE);
            mRecentSearchesFooter.setVisibility(View.GONE);
        } else {
            mRecentSearchesHeader.setVisibility(View.VISIBLE);
            mRecentSearchesFooter.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSearchViewMenuItem == null) {
            return;
        }
        BoxSearchView searchView = (BoxSearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);
        outState.putBoolean(RESTORE_SEARCH, !searchView.isIconified());
        outState.putString(SEARCH_QUERY, searchView.getQuery().toString());
    }

    private void clearSearch() {
        if (mSearchViewMenuItem == null) {
            return;
        }

        BoxSearchView searchView = (BoxSearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);
        if (!searchView.isIconified()) {
            searchView.onActionViewCollapsed();
            searchView.setIconified(true);
        }
    }

    @Override
    public void onItemClick(BoxItem item) {
        // If current fragment is search fragment, add search term to recent searches
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (fragment instanceof BoxSearchFragment) {
            mController.addToRecentSearches(this, mSession.getUser(), item.getName());
        }

        // If click is on a folder, navigate to that folder
        if (item instanceof BoxFolder) {
            mCurrentBoxFolder = (BoxFolder) item;
            handleBoxFolderClicked(mCurrentBoxFolder);
        }
    }

    @Override
    public void onSearchExpanded() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (fragment instanceof BoxSearchFragment) {
            return;
        }

        mRecentSearches = mController.getRecentSearches(this, mSession.getUser());
        mRecentSearchesAdapter = new BoxRecentSearchAdapter(this, mRecentSearches, new BoxRecentSearchAdapter.BoxRecentSearchListener() {
            @Override
            public void onCloseClicked(int position) {
                mRecentSearches.clear();
                mRecentSearches.addAll(mController.deleteFromRecentSearches(BoxBrowseActivity.this, mSession.getUser(), position));
                mRecentSearchesAdapter.notifyDataSetChanged();
                if (mRecentSearches.size() == 0) {
                    mRecentSearchesHeader.setVisibility(View.GONE);
                    mRecentSearchesFooter.setVisibility(View.GONE);
                }
            }
        });
        mRecentSearchesListView.setAdapter(mRecentSearchesAdapter);
        enableDisableRecentView();

    }

    @Override
    public void onSearchCollapsed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (fragment instanceof BoxSearchFragment) {
            getSupportFragmentManager().popBackStack();
        }
        enableDisableRecentView();
    }

    @Override
    public void onQueryTextChange(String text) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
        if (TextUtils.isEmpty(text) && !(fragment instanceof BoxSearchFragment)) {
            return;
        }

        mRecentSearchesListView.setVisibility(View.GONE);
        if (!(fragment instanceof BoxSearchFragment)) {
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

            // All fragments will always navigate into folders
            BoxSearchFragment searchFragment = new BoxSearchFragment.Builder(mSession, text, mCurrentBoxFolder).build();
            trans.replace(R.id.box_browsesdk_fragment_container, searchFragment)
                    .addToBackStack(BoxBrowseFragment.TAG)
                    .commit();
        } else {
            ((BoxSearchFragment)fragment).search(text);
        }
    }

    @Override
    public void onQueryTextSubmit(String text) {
        hideKeyboard();
    }

    private void hideKeyboard() {
        if (mSearchView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        }
    }

    /**
     * Helper method to initialize the activity with the default toolbar for the Share SDK.
     * This will show a material themed toolbar with a back button that will finish the Activity.
     */
    protected void initToolbar() {
        Toolbar actionBar = (Toolbar) findViewById(com.box.androidsdk.browse.R.id.box_action_bar);
        setSupportActionBar(actionBar);
        actionBar.setNavigationIcon(com.box.androidsdk.browse.R.drawable.ic_box_browsesdk_arrow_back_grey_24dp);
        actionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.box_browsesdk_fragment_container);
                if (fragment instanceof BoxSearchFragment) {
                    onBackPressed();
                    return;
                }

                if (mSearchView.isExpanded()) {
                    clearSearch();
                    return;
                }

                FragmentManager fragManager = getSupportFragmentManager();
                if (fragManager != null && fragManager.getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackStackChanged() {
        invalidateOptionsMenu();
        // Update title while navigating folders.
        setTitle(getCurrentFolder());
    }

    /**
     * Create a builder object that can be used to construct an intent to launch an instance of this activity.
     * This will be used to create an instance of BoxFolderActivity or a BoxFileActivity
     *
     * @param <R> the type parameter. It should be the derived class from IntentBuilder.
     *           This ensures return value is derived class object and there is no need to typecast the returned value.
     */
    protected static abstract class IntentBuilder<R> {

        final BoxSession mSession;
        final Context mContext;
        BoxFolder mFolder;

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
         * Sets the folder to be browsed
         *
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
         * Add extras.
         *
         * @param intent intent to add extras from this builder to.
         */
        protected void addExtras(final Intent intent) {

        }

        /**
         * Create launch intent intent.
         *
         * @return the intent
         */
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
