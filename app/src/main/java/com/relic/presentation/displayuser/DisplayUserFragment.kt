package com.relic.presentation.displayuser

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.relic.R
import com.relic.dagger.DaggerVMComponent
import com.relic.dagger.modules.AuthModule
import com.relic.dagger.modules.RepoModule
import com.relic.presentation.base.RelicFragment
import com.relic.presentation.displayuser.fragments.PostsFragment
import com.shopify.livedataktx.observe
import kotlinx.android.synthetic.main.display_user.view.*

class DisplayUserFragment : RelicFragment() {

    private val displayUserVM by lazy {
        ViewModelProviders.of(this, object : ViewModelProvider.Factory{
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return DaggerVMComponent.builder()
                    .repoModule(RepoModule(context!!))
                    .authModule(AuthModule(context!!))
                    .build()
                    .getDisplayUserVM().create(username) as T
            }
        }).get(DisplayUserVM::class.java)
    }

    private lateinit var username : String

    private lateinit var pagerAdapter: UserContentPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(ARG_USERNAME)?.let { username = it }

        pagerAdapter = UserContentPagerAdapter(childFragmentManager).apply {
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
            contentFragments.add(PostsFragment())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.display_user, container, false).apply {
            (userToolbar as Toolbar).title = getString(R.string.user_prefix_label, username)

            userViewPager.adapter = pagerAdapter
            userTabLayout.setupWithViewPager(userViewPager)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel(viewLifecycleOwner)
    }

    override fun bindViewModel(lifecycleOwner: LifecycleOwner) {
        displayUserVM.submissionLiveData.observe (lifecycleOwner){

        }
    }

    // region livedata handlers

    // endregion livedata handlers

    companion object {
        val ARG_USERNAME = "arg_username"

        fun create(username : String) : DisplayUserFragment {
            val bundle = Bundle().apply {
                putString(ARG_USERNAME, username)
            }

            return DisplayUserFragment().apply {
                arguments = bundle
            }
        }
    }

    private inner class UserContentPagerAdapter(fm : FragmentManager) : FragmentPagerAdapter(fm) {
        val contentFragmentTitles = listOf("Submissions", "Comments", "Saved", "Upvoted", "Downvoted", "Gilded", "Hidden")
        val contentFragments : ArrayList<Fragment> = ArrayList()

        override fun getItem(p0: Int): Fragment = contentFragments[p0]

        override fun getCount(): Int = contentFragments.size

        override fun getPageTitle(position: Int): CharSequence? {
            return contentFragmentTitles[position]
        }
    }
}