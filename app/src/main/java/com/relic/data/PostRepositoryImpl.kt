package com.relic.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.android.volley.NoConnectionError

import com.relic.R
import com.relic.data.deserializer.ParsedPostsData
import com.relic.data.deserializer.PostDeserializerImpl
import com.relic.network.NetworkRequestManager
import com.relic.data.gateway.PostGateway
import com.relic.data.gateway.PostGatewayImpl
import com.relic.network.request.RelicOAuthRequest
import com.relic.presentation.callbacks.RetrieveNextListingCallback
import com.relic.data.entities.ListingEntity
import com.relic.data.entities.PostEntity
import com.relic.data.entities.PostSourceEntity
import com.relic.data.models.PostModel
import com.relic.data.repository.RepoConstants.ENDPOINT
import com.relic.network.request.RelicRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.simple.parser.ParseException
import java.lang.Exception

import java.util.ArrayList

import javax.inject.Inject

/**
 * This repository is used for accessing posts either by:
 * a) getting a livedata reference for locally stored posts
 * b) retrieving posts from the network and storing them locally
 *
 * Note: Cancellation of parent coroutines automatically propagate downward to
 * all its children. When performing retrieval methods (eg. post retrieval) from
 * the network, the process should not be cancelled even if its parent coroutine
 * is cancelled.
 *
 * I initially designed the retrieval methods as non-suspending functions that internally
 * launched a coroutine from the Global scope but realized suspend functions offered more
 * benefits:
 * a) The method is poorly thought out and makes cancellation more difficult than necessary
 * b) Because the new methods are now suspending, the caller has control over the coroutine
 * scope. This is certainly more flexible as there are some (though rare) cases where we can
 * supply a non-global scope that we can close at our own convenience to cancel retrieval
 *
 *  As a result, the retrieval methods are now suspend functions
 */
class PostRepositoryImpl @Inject constructor(
    appContext: Context,
    private val requestManager: NetworkRequestManager
) : PostRepository {
    private val TAG = "POST_REPO"

    companion object {
        // keys for the "after" value for listings
        private const val KEY_FRONTPAGE = "frontpage"
        private const val KEY_ALL = "all"
        private const val KEY_OTHER = "other"
    }

    private val sortTypesWithScope = arrayOf(
        PostRepository.SortType.HOT,
        PostRepository.SortType.RISING,
        PostRepository.SortType.TOP
    )

    private val appDB: ApplicationDB = ApplicationDB.getDatabase(appContext)
    // TODO convert this to DI
    private val postDeserializer = PostDeserializerImpl(appContext)

    override val postGateway: PostGateway = PostGatewayImpl(appContext, requestManager)

    // region interface methods

    override fun getPosts(postSource: PostRepository.PostSource) : LiveData<List<PostModel>> {
        return when (postSource) {
            is PostRepository.PostSource.Subreddit -> appDB.postDao.getPostsFromSubreddit(postSource.subredditName)
            is PostRepository.PostSource.Frontpage -> appDB.postDao.getPostsFromFrontpage()
            is PostRepository.PostSource.User -> {
                when (postSource.retrievalOption) {
                    PostRepository.RetrievalOption.Submitted -> appDB.userPostingDao.getUserPosts()
                    PostRepository.RetrievalOption.Comments -> MutableLiveData()
                    PostRepository.RetrievalOption.Saved -> appDB.userPostingDao.getUserSavedPosts()
                    PostRepository.RetrievalOption.Upvoted -> appDB.userPostingDao.getUserUpvotedPosts()
                    PostRepository.RetrievalOption.Downvoted -> appDB.userPostingDao.getUserDownvotedPosts()
                    PostRepository.RetrievalOption.Gilded -> appDB.userPostingDao.getUserGilded()
                    PostRepository.RetrievalOption.Hidden -> appDB.userPostingDao.getUserHidden()
                }
            }
            else -> appDB.postDao.getPostsFromAll()
        }
    }

    override suspend fun retrieveMorePosts(
        postSource: PostRepository.PostSource,
        listingAfter: String
    ) {
        // change the api endpoint to access the next post listing
        val ending = when (postSource) {
            is PostRepository.PostSource.Subreddit -> "r/${postSource.subredditName}"
            is PostRepository.PostSource.User -> "user/${postSource.username}/${postSource.retrievalOption.name.toLowerCase()}"
            else -> ""
        }

        Log.d(TAG, "retrieve more posts : api url : $ENDPOINT$ending?after=$listingAfter")
        try {
            val response = requestManager.processRequest(
                method = RelicOAuthRequest.GET,
                url = "$ENDPOINT$ending?after=$listingAfter"
            )
            Log.d(TAG, "more posts $response")

            val listingKey = getListingKey(postSource)
            val parsedData = postDeserializer.parsePosts(response, postSource, listingKey)

            insertParsedPosts(parsedData)
        } catch (e: Exception) {
            throw DomainTransfer.handleException("retrieve more posts", e) ?: e
        }

    }

    override suspend fun getNextPostingVal(callback: RetrieveNextListingCallback, postSource: PostRepository.PostSource) {
        val key = getListingKey(postSource)

        withContext(Dispatchers.IO) {
            // get the "after" value for the most current sub listing
            val subAfter = appDB.listingDAO.getNext(key)
            callback.onNextListing(subAfter)
        }
    }

    override fun getPost(postFullName: String): LiveData<PostModel> {
        return appDB.postDao.getSinglePost(postFullName)
    }

    @Throws(RelicRequestError::class)
    override suspend fun retrieveSortedPosts(
        postSource: PostRepository.PostSource,
        sortType: PostRepository.SortType,
        sortScope: PostRepository.SortScope
    ) {
        // convert into a builder to make it easier to build api url
        // generate the ending of the request url based on the source type
        var ending = ENDPOINT + when (postSource) {
            is PostRepository.PostSource.Subreddit -> "r/${postSource.subredditName}"
            is PostRepository.PostSource.User -> {
                "user/${postSource.username}/${postSource.retrievalOption.name.toLowerCase()}?sort=${sortType.name.toLowerCase()}&t=${sortScope.name.toLowerCase()}"
            }
            else -> ""
        }

        // modify the endpoint based on the sorting options selected by the user
        if (sortType != PostRepository.SortType.DEFAULT && postSource !is PostRepository.PostSource.User) {
            // build the appropriate endpoint based on the "sort by" code and time scope
            ending += "/${sortType.name.toLowerCase()}/"

            // only add sort scope for the options that accept it
            if (sortTypesWithScope.contains(sortType)) ending += "?t=" + sortScope.name.toLowerCase()
        }

        Log.d(TAG, "retrieve sorted posts api url : $ending")
        coroutineScope {
            try {
                val response = requestManager.processRequest(
                    method = RelicOAuthRequest.GET,
                    url = ending
                )
                val clear = launch { clearAllPostsFromSource(postSource) }
                Log.d(TAG, "retrieve posts response :  $response")

                val listingKey = getListingKey(postSource)
                val parsedData = postDeserializer.parsePosts(response, postSource, listingKey)
                Log.d(TAG, "retrieve more posts : after ${parsedData.listingEntity.afterPosting}")

                clear.join()
                insertParsedPosts(parsedData)
            } catch (e: Exception) {
                throw DomainTransfer.handleException("retrieve account", e) ?: e
            }
        }
    }

    override suspend fun retrievePost(
        subredditName: String,
        postFullName: String,
        postSource: PostRepository.PostSource
    ) {
        val ending = "r/$subredditName/comments/${postFullName.substring(3)}"

        try {
            val response = requestManager.processRequest(
                method = RelicOAuthRequest.GET,
                url = ENDPOINT + ending
            )

            postDeserializer.parsePost(response).apply {
                postEntity.visited = true

                withContext(Dispatchers.IO) {
                    appDB.postDao.insertPost(postEntity)
                    appDB.postSourceDao.insertPostSources(listOf(postSourceEntity))
                }
            }

        } catch (e: Exception) {
            throw DomainTransfer.handleException("retrieve account", e) ?: e
        }
    }

    override suspend fun clearAllPostsFromSource(postSource: PostRepository.PostSource) {
        withContext(Dispatchers.IO) {
            when (postSource) {
                is PostRepository.PostSource.Frontpage -> appDB.postSourceDao.removeAllFrontpageAsSource()
                is PostRepository.PostSource.All -> appDB.postSourceDao.removeAllAllAsSource()
                is PostRepository.PostSource.Subreddit -> appDB.postSourceDao.removeAllSubredditAsSource(postSource.subredditName)
                is PostRepository.PostSource.User -> {
                    appDB.postSourceDao.apply {
                        when (postSource.retrievalOption) {
                            PostRepository.RetrievalOption.Submitted -> removeAllUserSubmittedAsSource()
                            PostRepository.RetrievalOption.Comments -> removeAllUserCommentsAsSource()
                            PostRepository.RetrievalOption.Saved -> removeAllUserSavedAsSource()
                            PostRepository.RetrievalOption.Upvoted -> removeAllUserUpvotedAsSource()
                            PostRepository.RetrievalOption.Downvoted -> removeAllUserDownvotedAsSource()
                            PostRepository.RetrievalOption.Gilded -> removeAllUserGildedAsSource()
                            PostRepository.RetrievalOption.Hidden -> removeAllUserHiddenAsSource()
                        }
                    }
                }
            }

            // remove all the source entities that no longer correspond to any remaining posts
            appDB.postSourceDao.removeAllUnusedSources()
        }
    }

    // endregion interface methods


    private suspend fun insertParsedPosts(parsedPosts : ParsedPostsData) {
        withContext(Dispatchers.IO) {
            parsedPosts.apply {
                if (postEntities.isNotEmpty()) appDB.postDao.insertPosts(postEntities)
                if (commentEntities.isNotEmpty()) appDB.commentDAO.insertComments(commentEntities)

                appDB.postSourceDao.insertPostSources(postSourceEntities)
                appDB.listingDAO.insertListing(listingEntity)
            }
        }
    }

    /**
     * builds the key used for retrieving the "after" value for a listing using its associated
     * post source
     */
    private fun getListingKey(postSource : PostRepository.PostSource) : String {
        return when (postSource) {
            is PostRepository.PostSource.Subreddit -> postSource.subredditName
            is PostRepository.PostSource.Frontpage -> KEY_FRONTPAGE
            is PostRepository.PostSource.All -> KEY_ALL
            is PostRepository.PostSource.User -> postSource.username + postSource.retrievalOption.name
            else -> KEY_OTHER
        }
    }
}
