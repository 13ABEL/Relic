package com.relic.presentation.main

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.relic.R
import com.relic.data.PostSource
import com.relic.domain.models.AccountModel
import com.relic.domain.models.UserModel
import com.relic.initializeNavHostFragments
import com.relic.preference.ViewPreferencesManager
import com.relic.presentation.base.RelicActivity
import com.relic.presentation.base.RelicFragment
import com.relic.presentation.displaypost.DisplayPostFragmentArgs
import com.relic.presentation.displaysub.DisplaySubFragmentArgs
import com.relic.presentation.displaysub.NavigationData
import com.relic.presentation.displayuser.DisplayUserFragmentArgs
import com.relic.presentation.displayuser.DisplayUserPreview
import com.relic.presentation.editor.ReplyEditorFragmentArgs
import com.relic.presentation.home.frontpage.MultiFragmentArgs
import com.relic.presentation.login.LoginActivity
import com.relic.presentation.media.DisplayGfycatFragmentArgs
import com.relic.presentation.media.DisplayImageFragmentArgs
import com.relic.presentation.media.DisplayVideoFragmentArgs
import com.relic.presentation.preferences.PreferenceLink
import com.relic.presentation.preferences.PreferencesActivity.Companion.KEY_RESULT_PREF_LINKS
import com.relic.presentation.subinfodialog.SubInfoBottomSheetDialog
import com.relic.presentation.util.MediaType
import com.relic.presentation.util.RequestCodes
import com.relic.presentation.util.observeConsumable
import com.relic.resetBottomNavigation
import com.shopify.livedataktx.observe
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

private const val SELECTED_NAV_ITEM_ID_KEY = "SELECTED_NAV_ITEM_ID_KEY"

class MainActivity : RelicActivity() {
    @Inject
    lateinit var factory: MainVM.Factory

    @Inject
    lateinit var viewPrefsManager: ViewPreferencesManager

    private val mainVM by lazy {
        ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return factory.create() as T
            }
        }).get(MainVM::class.java)
    }

    private val menuToDestinationMap = mapOf(
        Pair(R.id.nav_account, R.id.displayUserFragment),
        Pair(R.id.nav_subs, R.id.displaySubsFragment),
        Pair(R.id.nav_home, R.id.homeFragment),
        Pair(R.id.nav_search, R.id.searchFragment),
        Pair(R.id.nav_settings, R.id.settingsFragment)
    )

    private lateinit var relicGD: GestureDetectorCompat

    private var itemSelectedDelegate: ((item: MenuItem?) -> Boolean)? = null
    private var navControllerLiveData: LiveData<NavController>? = null
    private var currentInitDialog: Dialog? = null

    // region lifecycle hooks
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme()
        setContentView(R.layout.activity_main)
        relicGD = GestureDetectorCompat(this, GestureDetector.SimpleOnGestureListener())

        // set up bottom nav if no saved instance state
        if (savedInstanceState == null) setupBottomNav()

        bindViewModel(this)
        if (!mainVM.isAuthenticated()) {
            displayLoginDialog()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        // set up after bottom navigation state has been restored
        val selectedItemId = savedInstanceState?.getInt(SELECTED_NAV_ITEM_ID_KEY)
        setupBottomNav(selectedItemId = selectedItemId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_NAV_ITEM_ID_KEY, bottom_navigation.selectedItemId)
        super.onSaveInstanceState(outState)
    }


    private fun bindViewModel(lifecycleOwner: LifecycleOwner) = mainVM.apply {
        accountsLiveData.observe(lifecycleOwner) { accounts -> accounts?.let { handleAccounts(it) } }
        userChangedEventLiveData.observeConsumable(lifecycleOwner) { handleUserChangedEvent(it) }
        navigationEventLiveData.observeConsumable(lifecycleOwner) { handleNavigationEvent(it) }
        initializationMessageLiveData.observe(lifecycleOwner) { handleInitMessage(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCodes.CHANGED_PREFERENCES -> {
                data?.getParcelableArrayListExtra<PreferenceLink>(KEY_RESULT_PREF_LINKS)?.let {
                    handlePreferenceChanges(it)
                }
            }
            RequestCodes.CHANGED_ACCOUNT -> {
                // inform vm the account has changed if the login was successful
                when (resultCode) {
                    Activity.RESULT_OK -> mainVM.onAccountSelected()
                    else -> displayLoginDialog()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // need to return true is the touch event is intercepted
        return if (relicGD.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return itemSelectedDelegate?.let {
            it(item)
        } ?: super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.apply {
            (fragments.last() as? RelicFragment?)?.let { relicFragment ->
                val handled = relicFragment.onBackPressed()
                if (!handled) super.onBackPressed()
            }
        }
    }

    // endregion lifecycle hooks

    // use the solution from the google navigation components repo for now since there isn't
    // a clear solution for managing multiple backstacks currently
    private fun setupBottomNav(selectedItemId: Int? = null) {
        // setup default nav item is selected item id is not provided
        val itemId = selectedItemId ?: R.id.nav_home

        bottom_navigation.apply {
            navControllerLiveData = initializeNavHostFragments(
                fragmentManager = supportFragmentManager,
                containerId = R.id.main_content_frame,
                shouldRestore = selectedItemId != null,
                initialItemId = itemId,
                menuItemToDestinationMap = menuToDestinationMap,
                onItemReselected = { item -> onBottomNavItemReselected(item) }
            )
        }
    }

    private fun onBottomNavItemReselected(menuItem: MenuItem) {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.apply {
            (fragments.last() as? RelicFragment?)?.let { relicFragment ->
                val handled = relicFragment.handleNavReselected()
                if (!handled) onBackPressed()
            }
        }
    }

    private fun setTheme() {
        val themeId = viewPrefsManager.getAppTheme()
        setTheme(themeId)
    }

    private fun handlePreferenceChanges(changedPreferenceLinks: ArrayList<PreferenceLink>) {
        // TODO handle each set of changed preferences properly when we add new ones
        recreate()
    }

    private fun displayLoginDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.welcome_dialog_title)
            .setMessage(R.string.welcome_dialog_body)
            .setCancelable(false)
            .setPositiveButton(R.string.welcome_dialog_login) { _, _ ->
                LoginActivity.startForResult(this@MainActivity)
            }
            .create()
            .show()
    }

// region livedata handlers

    private fun handleUserChangedEvent(newUser: UserModel) {
        // here we want to reset the bottom nav without restoring it
        bottom_navigation.resetBottomNavigation()
        setupBottomNav(null)
    }

    private fun handleAccounts(accounts: List<AccountModel>) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val left = resources.getDimension(R.dimen.padding_xl).toInt()
            val top = resources.getDimension(R.dimen.padding_l).toInt()
            setMargins(left, top, left, 0)
        }

        // create option to change account for each available account
        for (account in accounts) {
            TextView(this).apply {
                text = account.name
                layoutParams = params
            }
        }
    }

    private fun handleInitMessage(message: String?) {
        if (message == null) {
            // dismiss current dialog
            currentInitDialog?.dismiss()
            currentInitDialog = null
        } else {
            currentInitDialog = AlertDialog.Builder(this)
                .setTitle("Welcome to Relic!")
                .setMessage(message)
                .setCancelable(false)
                .create().apply {
                    show()
                }
        }
    }

    private fun handleNavigationEvent(navData: NavigationData) {
        when (navData) {
            // navigates to display post
            is NavigationData.ToPost -> {
                val args = DisplayPostFragmentArgs(postFullName = navData.postFullname, subredditName = navData.subredditName, enableVisitSub = true).toBundle()
                navControllerLiveData?.value?.navigate(R.id.displayPostFragment, args)
            }
            is NavigationData.ToUser -> {
                val args = DisplayUserFragmentArgs(navData.username).toBundle()
                navControllerLiveData?.value?.navigate(R.id.displayUserFragment, args)
            }
            // navigates to display image on top of current fragment
            is NavigationData.ToImage -> {
                val args = DisplayImageFragmentArgs(url = navData.thumbnail).toBundle()
                navControllerLiveData?.value?.navigate(R.id.displayImageFragment, args)
            }
            // let browser handle navigation to url
            is NavigationData.ToExternal -> {
                val openInBrowser = Intent(Intent.ACTION_VIEW, Uri.parse(navData.url))
                startActivity(openInBrowser)
            }
            is NavigationData.ToUserPreview -> {
                DisplayUserPreview.create(navData.username)
                    .show(supportFragmentManager, TAG)
            }
            is NavigationData.ToPostSource -> {
                when (navData.source) {
                    is PostSource.Subreddit -> {
                        val args = DisplaySubFragmentArgs(subName = navData.source.getSourceName()).toBundle()
                        navControllerLiveData?.value?.navigate(R.id.displaySubFragment, args)
                    }
                    is PostSource.Frontpage, PostSource.All, PostSource.Popular -> {
                        val args = MultiFragmentArgs(multiName = navData.source.getSourceName()).toBundle()
                        navControllerLiveData?.value?.navigate(R.id.multiFragment, args)
                    }
                    is PostSource.All -> {
                        navControllerLiveData?.value?.navigate(R.id.multiFragment)
                    }
                    is PostSource.Popular -> {
                        navControllerLiveData?.value?.navigate(R.id.multiFragment)
                    }
                }

            }
            is NavigationData.PreviewPostSource -> {
                SubInfoBottomSheetDialog.create(navData.source.getSourceName())
                    .show(supportFragmentManager, TAG)
            }

            is NavigationData.ToMedia -> openMedia(navData)
            is NavigationData.ToReply -> openPostReplyEditor(navData.parentFullname)
        }
    }

// endregion livedata handlers

    private fun openMedia(navMediaData: NavigationData.ToMedia) {
        when (val mediaType = navMediaData.mediaType) {
            is MediaType.Gfycat -> {
                val args = DisplayGfycatFragmentArgs(mediaType.mediaUrl).toBundle()
                navControllerLiveData?.value?.navigate(R.id.displayGfycatFragment, args)
            }
            is MediaType.VReddit -> {
                val args = DisplayVideoFragmentArgs(url = mediaType.mediaUrl, audioUrl = mediaType.audioUrl).toBundle()
                navControllerLiveData?.value?.navigate(R.id.displayVideoFragment, args)
            }
            else -> {
                Toast.makeText(baseContext, "Media type $navMediaData doesn't have a handler yet", Toast.LENGTH_SHORT).show()
            }
        }
        // TODO handle additional cases and add support for custom plugins
    }

    private fun openPostReplyEditor(parentFullname: String) {
        val args = ReplyEditorFragmentArgs(parentFullname, true).toBundle()
        navControllerLiveData?.value?.navigate(R.id.replyEditorFragment, args)
    }

}


