package com.relic.presentation.displaypost.comments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.relic.R
import com.relic.domain.models.CommentModel
import com.relic.interactor.CommentInteraction
import com.relic.interactor.Contract
import com.relic.presentation.base.ItemNotifier
import com.relic.presentation.displaypost.DisplayPostContract
import kotlinx.android.synthetic.main.relic_more_comments_item.view.*

class RelicCommentMoreItemsView(
    context: Context,
    attrs : AttributeSet? = null,
    defStyleAttr : Int = 0
) : RelativeLayout (context, attrs, defStyleAttr) {

    lateinit var comment: CommentModel

    init {
        LayoutInflater.from(context).inflate(R.layout.relic_more_comments_item, this)
    }

    fun setLoadMore(comment : CommentModel) {
        this.comment = comment
        val moreToLoad = comment.more?.size ?: 0
        loadMoreItemText.text = resources.getString(R.string.load_comments, moreToLoad)

        if (comment.depth >= 0){
            displayReplyDepth(comment.depth)
        }
    }

    fun setViewDelegate(
        delegate: DisplayPostContract.LoadMoreCommentsDelegate,
        notifier: ItemNotifier
    ) {
        setOnClickListener {
            delegate.onExpandReplies(comment)
            notifier.notifyItem()
        }
    }

    fun displayReplyDepth(depth : Int) {
        // width, height
        val indentParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.padding_s) * depth
            bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_xxxs)
        }

        loadMoreItemRoot.layoutParams = indentParams
    }
}