package com.relic.data

import android.arch.lifecycle.LiveData

import com.relic.data.models.CommentModel

interface CommentRepository {

    /**
     * Exposes the comments as a liveData list
     * @param postFullName full name of a post
     * @param displayNRows if greater than 0, displays top x comments loaded
     */
    fun getComments(postFullName: String, displayNRows: Int = 0): LiveData<List<CommentModel>>

    /**
     * retrieves comments for a post from the network and stores them locally
     * @param subName display name of a subreddit
     * @param postFullName id of a post
     * @param refresh whether to refresh comments or get next
     */
    suspend fun retrieveComments(subName: String, postFullName: String, refresh : Boolean)

    suspend fun retrieveCommentChildren(commentModel: CommentModel)

    /**
     * clears all locally stored comments
     * @param postFullName full name of the post to clear the comments for
     */
    fun clearComments(postFullName: String)

    fun getReplies(parentId : String) : LiveData<List<CommentModel>>
}
