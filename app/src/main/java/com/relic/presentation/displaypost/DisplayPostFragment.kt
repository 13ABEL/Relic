package com.relic.presentation.displaypost

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.relic.R
import com.relic.data.PostSource
import com.relic.domain.models.PostModel
import com.relic.presentation.base.RelicFragment
import com.relic.presentation.displaypost.tabs.CommentsFragment
import com.relic.presentation.displaypost.tabs.FullPostFragment
import com.relic.presentation.displaysub.DisplaySubFragment
import com.relic.presentation.displayuser.DisplayUserPreview
import com.relic.presentation.editor.ReplyEditorFragment
import com.relic.presentation.media.DisplayGfycatFragment
import com.relic.presentation.media.DisplayImageFragment
import com.relic.presentation.util.MediaType
import com.shopify.livedataktx.nonNull
import com.shopify.livedataktx.observe
import kotlinx.android.synthetic.main.display_post.*
import kotlinx.android.synthetic.main.tabtitle_comment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

class DisplayPostFragment : RelicFragment(), CoroutineScope {
    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    @Inject lateinit var factory : DisplayPostVM.Factory

    private val displayPostVM : DisplayPostVM by lazy {
        ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return factory.create(subredditName, postFullName, postSource) as T
            }
        }).get(DisplayPostVM::class.java)
    }

    private lateinit var postFullName: String
    private lateinit var subredditName: String
    private lateinit var postSource: PostSource
    private var enableVisitSub = false

    private lateinit var pagerAdapter : DisplayPostPagerAdapter
    private lateinit var tabTitleView : View
    private var previousError : PostErrorData? = null

    // region lifecycle hooks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.apply {
            getString(ARG_POST_FULLNAME)?.let { postFullName = it }
            getString(ARG_SUB_NAME)?.let { subredditName = it }
            getParcelable<PostSource>(ARG_POST_SOURCE)?.let { postSource = it }
            enableVisitSub = getBoolean(ARG_ENABLE_VISIT_SUB)
        }

        pagerAdapter = DisplayPostPagerAdapter(childFragmentManager).apply {
            tabFragments.add(FullPostFragment())
            tabFragments.add(CommentsFragment())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.display_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).apply {
            setSupportActionBar(displayPostToolbar as Toolbar)
        }

        initializeToolbar()

        displayPostViewPager.apply {
            adapter = pagerAdapter
            displayPostTabLayout.setupWithViewPager(this)
        }

        // use a custom view for the comment tab title
        val commentTabPos = 1
        displayPostTabLayout.getTabAt(commentTabPos)?.apply {
            tabTitleView = LayoutInflater.from(context).inflate(R.layout.tabtitle_comment, null)
            customView = tabTitleView
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.clear()
        inflater.inflate(R.menu.display_post_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var override = true

        when (item.itemId) {
            R.id.post_menu_reply -> openPostReplyEditor(postFullName)
            else -> override = super.onOptionsItemSelected(item)
        }

        return override
    }

    // endregion lifecycle hooks

    override fun bindViewModel(lifecycleOwner: LifecycleOwner) {
        displayPostVM.apply {
            postNavigationLiveData.nonNull().observe(lifecycleOwner) { handleNavigation(it) }
            errorLiveData.observe(lifecycleOwner) { handleError(it) }
            postLiveData.nonNull().observe(lifecycleOwner) { handlePost(it) }
        }
    }

    // region live data handlers

    private fun handleNavigation(navigationData : PostNavigationData) {
        when (navigationData) {
            is PostNavigationData.ToMedia -> openMedia(navigationData)
            is PostNavigationData.ToReply -> openPostReplyEditor(navigationData.parentFullname)
            is PostNavigationData.ToURL -> {
                Intent(Intent.ACTION_VIEW).apply{
                    data = Uri.parse(navigationData.url)
                    startActivity(this)
                }
            }
            is PostNavigationData.ToUserPreview -> {
                DisplayUserPreview.create(navigationData.username)
                    .show(requireFragmentManager(), TAG)
            }
        }
    }

    private fun handleError(error : PostErrorData?) {
        if (previousError != error) {
            // default details for unhandled exceptions to be displayed
            var snackbarMessage = resources.getString(R.string.unknown_error)
            var displayLength = Snackbar.LENGTH_SHORT
            var actionMessage: String? = null
            var action: () -> Unit = {}

            when (error) {
                is PostErrorData.NetworkUnavailable -> {
                    snackbarMessage = resources.getString(R.string.network_unavailable)
                    displayLength = Snackbar.LENGTH_INDEFINITE
                    actionMessage = resources.getString(R.string.refresh)
                    action = { displayPostVM.refreshData() }
                }
            }

            snackbar = Snackbar.make(displayPostRootView, snackbarMessage, displayLength).apply {
                actionMessage?.let {
                    setAction(it) { action.invoke() }
                }
                show()
            }
        }
    }

    private fun handlePost(post : PostModel) {
        tabTitleCommentCount.text = post.commentCount.toString()
    }

    // endregion live data handlers

    private fun initializeToolbar() {
        val pActivity = (activity as AppCompatActivity)

        pActivity.supportActionBar?.apply {
            title = subredditName

            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        (displayPostToolbar as Toolbar).apply {
            setNavigationOnClickListener { activity?.onBackPressed() }
            if (enableVisitSub) setOnClickListener {
                val subFragment = DisplaySubFragment.create(subredditName)
                activity!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content_frame, subFragment).addToBackStack(TAG).commit()
            }
        }
    }

    private fun openMedia(navMediaData : PostNavigationData.ToMedia) {
        val displayFragment = when (navMediaData.mediaType)  {
            MediaType.Gfycat -> DisplayGfycatFragment.create(navMediaData.mediaUrl)
            else -> DisplayImageFragment.create(navMediaData.mediaUrl)
        }
        activity!!.supportFragmentManager
                .beginTransaction()
                .add(R.id.main_content_frame, displayFragment)
                .addToBackStack(TAG)
                .commit()
    }

    private fun openPostReplyEditor(parentFullname: String) {
        // this option is for replying to parent
        // Should also allow user to do it inline, but that can be saved for a later task
        val editorFragment = ReplyEditorFragment.create(parentFullname, true)

        // replace the current screen with the newly created fragment
        activity!!.supportFragmentManager.beginTransaction()
                .replace(R.id.main_content_frame, editorFragment).addToBackStack(TAG).commit()
    }

    fun onPostDataLoaded() {
        Toast.makeText(context, "Comments loaded", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "DISPLAYPOST_VIEW"
        private const val ARG_POST_FULLNAME = "full_name"
        private const val ARG_SUB_NAME = "subreddit"
        private const val ARG_POST_SOURCE = "post_source"
        private const val ARG_ENABLE_VISIT_SUB = "enable_visit_sub"

        /**
         * @param enableVisitSub used to allow onClicks to subreddit. Should only be enabled when
         * visiting post from different source than its sub (ie frontpage, all, etc) to prevent
         * continuously chaining open subreddit actions
         */
        fun create(postId : String, subreddit : String, postSource: PostSource, enableVisitSub : Boolean = false) : DisplayPostFragment {
            // create a new bundle for the post id
            val bundle = Bundle().apply {
                putString(ARG_POST_FULLNAME, postId)
                putString(ARG_SUB_NAME, subreddit)
                putBoolean(ARG_ENABLE_VISIT_SUB, enableVisitSub)
                putParcelable(ARG_POST_SOURCE, postSource)
            }

            return DisplayPostFragment().apply {
                arguments = bundle
            }
        }
    }

    private inner class DisplayPostPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        val tabFragmentTitles = listOf("POST", "COMMENTS")
        val tabFragments = ArrayList<Fragment>()

        override fun getPageTitle(position: Int): CharSequence? {
            return tabFragmentTitles[position]
        }

        override fun getCount() = tabFragments.size

        override fun getItem(position: Int): Fragment {
            return tabFragments[position]
        }
    }
}
