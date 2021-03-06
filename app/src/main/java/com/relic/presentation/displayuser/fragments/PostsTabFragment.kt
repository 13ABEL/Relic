package com.relic.presentation.displayuser.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.relic.R
import com.relic.domain.models.ListingItem
import com.relic.interactor.Contract
import com.relic.preference.ViewPreferencesManager
import com.relic.presentation.base.RelicFragment
import com.relic.presentation.displayuser.DisplayUserFragment
import com.relic.presentation.displayuser.ErrorData
import com.relic.presentation.displayuser.UserTab
import com.shopify.livedataktx.nonNull
import com.shopify.livedataktx.observe
import kotlinx.android.synthetic.main.display_user_submissions.*
import javax.inject.Inject

class PostsTabFragment : RelicFragment() {

    @Inject
    lateinit var viewPrefsManager: ViewPreferencesManager

    @Inject
    lateinit var postInteractor: Contract.PostAdapterDelegate

    @Inject
    lateinit var commentInteractor: Contract.CommentAdapterDelegate

    private val postsTabVM by lazy {
        (requireParentFragment() as DisplayUserFragment).displayUserVM
    }

    lateinit var selectedUserTab: UserTab

    private lateinit var userPostsAdapter: ListingItemAdapter
    private var scrollLocked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().getParcelable<UserTab>(ARG_USER_TAB)?.let { userTab ->
            selectedUserTab = userTab
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.display_user_submissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPostsAdapter = ListingItemAdapter(viewPrefsManager, postInteractor, commentInteractor)
        userTabRecyclerView.apply {
            adapter = userPostsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        userTabSwipeRefreshLayout.isRefreshing = true

        attachScrollListeners()
    }

    override fun bindViewModel(lifecycleOwner: LifecycleOwner) {
        // subscribe to the appropriate livedata based on tab selected
        postsTabVM.getTabPostsLiveData(selectedUserTab).nonNull().observe(lifecycleOwner) {
            handleListingItems(it)
        }
        postsTabVM.errorLiveData.nonNull().observe(lifecycleOwner) { handleError(it) }
    }

    override fun handleNavReselected(): Boolean {
        // for double click, second click will try to access view when removed
        userTabRecyclerView ?: return false
        return when (userTabRecyclerView.canScrollVertically(-1)) {
            true -> {
                // can still scroll up, so reselection should scroll to top
                userTabRecyclerView.smoothScrollToPosition(0)
                true
            }
            false -> {
                // already at top, we don't handle
                false
            }
        }
    }

    private fun attachScrollListeners() {
        // attach listener for checking if the user has scrolled to the bottom of the recyclerview
        userTabRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && !scrollLocked) {
                    // lock scrolling until set of posts are loaded to prevent additional unwanted retrievals
                    scrollLocked = true
                    tabProgress.visibility = View.VISIBLE
                    // fetch the next post listing
                    postsTabVM.requestPosts(selectedUserTab, false)
                }
            }
        })

        userTabSwipeRefreshLayout.setOnRefreshListener {
            resetRecyclerView()
            postsTabVM.requestPosts(selectedUserTab, true)
        }
    }

    private fun handleListingItems(listingItems: List<ListingItem>) {
        tabProgress.visibility = View.GONE
        userPostsAdapter.setItems(listingItems)
        userTabSwipeRefreshLayout.isRefreshing = false
        scrollLocked = false
    }

    fun resetRecyclerView() {
        // empties current items to show that it's being refreshed
        userTabRecyclerView.layoutManager?.scrollToPosition(0)
        userPostsAdapter.clear()

        userTabSwipeRefreshLayout.isRefreshing = true
    }

    private fun handleError(errorData: ErrorData) {
        when (errorData) {
            is ErrorData.NoMorePosts -> {
                if (selectedUserTab == errorData.tab) {
                    Snackbar.make(userTabRoot, "No more posts loaded for ${selectedUserTab.tabName}", Snackbar.LENGTH_SHORT).show()
                    tabProgress.visibility = View.GONE
                    userTabSwipeRefreshLayout.isRefreshing = false
                }
            }
        }

    }

    companion object {
        private val ARG_USER_TAB = "arg_user_tab"

        fun create(userTab: UserTab): PostsTabFragment {
            val bundle = Bundle()
            bundle.putParcelable(ARG_USER_TAB, userTab)

            return PostsTabFragment().apply {
                arguments = bundle
            }
        }
    }
}