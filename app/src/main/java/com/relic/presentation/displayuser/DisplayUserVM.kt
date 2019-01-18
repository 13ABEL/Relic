package com.relic.presentation.displayuser

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.relic.data.CommentRepository
import com.relic.data.ListingRepository
import com.relic.data.PostRepository
import com.relic.data.models.PostModel
import com.relic.presentation.callbacks.RetrieveNextListingCallback
import com.relic.presentation.displaysub.DisplaySubContract
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class DisplayUserVM(
    private val postRepo: PostRepository,
    private val commentRepo: CommentRepository,
    private val listingRepo: ListingRepository,
    private val username : String
) : ViewModel(), DisplaySubContract.PostAdapterDelegate {

    class Factory @Inject constructor(
        private val postRepo: PostRepository,
        private val commentRepo: CommentRepository,
        private val listingRepo: ListingRepository
    ) {
        fun create(username : String) : DisplayUserVM {
            return DisplayUserVM(postRepo, commentRepo, listingRepo, username)
        }
    }

    private var currentSortingType = emptyMap<UserTab, PostRepository.SortType>()
    private var currentSortingScope = emptyMap<UserTab, PostRepository.SortScope>()

    private val _submissionLiveData  = postRepo.getPosts(PostRepository.PostSource.CurrentUser)
    val submissionLiveData : LiveData<List<PostModel>> = _submissionLiveData

    init {
        GlobalScope.launch {
            postRepo.retrieveUserSubmissions(username)
        }
    }

    /**
     *
     */
    fun requestPosts(tab : UserTab, refresh : Boolean) {
        // subscribe to the appropriate livedata based on tab selected
        val postSource: PostRepository.PostSource = when (tab) {
            is UserTab.Submissions -> PostRepository.PostSource.CurrentUser
            is UserTab.Comments -> PostRepository.PostSource.CurrentUser
            is UserTab.Saved -> PostRepository.PostSource.CurrentUser
            is UserTab.Upvoted -> PostRepository.PostSource.CurrentUser
            is UserTab.Downvoted -> PostRepository.PostSource.CurrentUser
            is UserTab.Gilded -> PostRepository.PostSource.CurrentUser
            is UserTab.Hidden -> PostRepository.PostSource.CurrentUser
        }

        GlobalScope.launch {
            if (refresh) {
                runBlocking { postRepo.clearAllPostsFromSource(postSource) }
                postRepo.retrieveUserSubmissions(username)
            } else {
                // not a fan of this design, because it requires the viewmodel to be aware of the
                // "key" being used to store the "after" value which is an implementation detail.
                // TODO consider refactoring later, for now be consistent
                val key = suspendCoroutine<String> { cont ->
                    postRepo.getNextPostingVal(
                        postSource = PostRepository.PostSource.CurrentUser,
                        callback = RetrieveNextListingCallback { afterVal ->
                            cont.resumeWith(Result.success(afterVal))
                        })
                }
                postRepo.retrieveMorePosts(postSource, key)
            }
        }
    }

    // region post adapter delegate

    override fun visitPost(postFullname: String, subreddit: String) {
    }

    override fun voteOnPost(postFullname: String, voteValue: Int) {
    }

    override fun savePost(postFullname: String, save: Boolean) {
    }

    override fun onThumbnailClicked(postThumbnailUrl: String) {
    }

    // endregion post adapter delegate
}