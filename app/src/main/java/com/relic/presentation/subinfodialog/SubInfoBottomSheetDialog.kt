package com.relic.presentation.subinfodialog

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.relic.R
import com.relic.domain.models.SubredditModel
import com.relic.presentation.base.RelicBottomSheetDialog
import com.relic.presentation.subinfodialog.SubInfoDialogContract.Companion.ARG_SUB_NAME
import com.shopify.livedataktx.nonNull
import com.shopify.livedataktx.observe
import kotlinx.android.synthetic.main.display_subinfo_sheetdialog.*
import timber.log.Timber
import javax.inject.Inject

class SubInfoBottomSheetDialog : RelicBottomSheetDialog() {
    @Inject
    lateinit var factory : SubInfoDialogVM.Factory

    private val viewModel : SubInfoDialogVM by lazy {
        ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                // inject dependencies into factory and construct viewmodel
                return factory.create(subName) as T
            }
        }).get(SubInfoDialogVM::class.java)
    }

    private lateinit var subName : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(ARG_SUB_NAME)?.apply {
            subName = this
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.display_subinfo_sheetdialog, container,  false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindVm()
        subNameView.text = resources.getString(R.string.sub_prefix_name, subName)

        // initialize onclicks
        subInfoSubView.setOnClickListener { }
        subInfoPinView.setOnClickListener { viewModel.pinSubreddit(true) }
    }

    private fun bindVm() {
        viewModel.subredditLiveData.nonNull().observe(this) { setSubredditData(it) }
        viewModel.sideBarLiveData.nonNull().observe(this) {
            Timber.d("Sidebar $it")
        }
    }

    private fun setSubredditData(subredditModel: SubredditModel) {
        if (subredditModel.isSubscribed) {
//            subscribeButtonView.text = getString(R.string.subscribed)
            subInfoSubView.background?.setTint(resources.getColor(R.color.positive))
        } else {
//            subscribeButtonView.text = getString(R.string.subscribe)
            subInfoSubView.background?.setTint(resources.getColor(R.color.negative))
        }

        subCountView.text = resources.getString(R.string.subscriber_count, subredditModel.subscriberCount)
        subDescriptionView.text = Html.fromHtml(Html.fromHtml(subredditModel.description ?: "").toString())
        // TODO create custom movement method class
        subDescriptionView.movementMethod
    }

    companion object {
        fun create(subreddit : String) : SubInfoBottomSheetDialog{
            return SubInfoBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUB_NAME, subreddit)
                }
            }
        }
    }

}