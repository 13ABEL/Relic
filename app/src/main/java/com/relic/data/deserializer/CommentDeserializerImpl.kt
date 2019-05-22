package com.relic.data.deserializer

import android.text.Html
import android.util.Log
import com.google.gson.GsonBuilder
import com.relic.data.entities.CommentEntity
import com.relic.data.entities.ListingEntity
import com.relic.data.models.CommentModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

// TODO convert to object and add interface so this can be injected
object CommentDeserializer : Contract.CommentDeserializer {
    private val TAG = "COMMENT_DESERIALIZER"

    private val jsonParser: JSONParser = JSONParser()
    private val gson = GsonBuilder().create()
    private val formatter = SimpleDateFormat("MMM dd',' hh:mm a", Locale.CANADA)
    private val currentYear = Date().year

    // region interface methods

    override suspend fun parseCommentsResponse(
        postFullName: String,
        response: String
    ) : ParsedCommentData {
        // the comment data is nested as the first element within an array
        val requestData = jsonParser.parse(response) as JSONArray
        val parentPostId = CommentDeserializer.removeTypePrefix(postFullName)

        return CommentDeserializer.parseComments(parentPostId, requestData[1] as JSONObject)
    }

    /**
     * Only use this method to parse the return from "morechildren" since it uses a different
     * format than the traditional method for retrieving comments
     *
     */
    override suspend fun parseMoreCommentsResponse(
        moreChildrenComment: CommentModel,
        response: String
    ) : List<CommentEntity> {
        val requestJson = (jsonParser.parse(response) as JSONObject)["json"] as JSONObject

        val requestData = requestJson["data"] as JSONObject
        val requestComments = requestData["things"] as JSONArray

        // calculate the depth of the comments (should be the same as the "load more")
        val depth = moreChildrenComment.depth
        val scale = 10f.pow(-(depth))
        var commentCount = 0

        Log.d(TAG, "load more scale ${moreChildrenComment.position}")

        return requestComments.fold(mutableListOf()) { accum, requestComment : Any? ->
            val commentJson = requestComment as JSONObject
            val childKind = commentJson["kind"] as String?

            val unmarshalledComments = if (childKind == "more") {
                // means there is a "more object"
                val moreData = (commentJson["data"] as JSONObject)

                val moreComment = unmarshallMore(
                    moreData,
                    moreChildrenComment.parentPostId,
                    moreChildrenComment.position + commentCount*scale
                )
                commentCount += 1
                listOf(moreComment)
            } else {
                unmarshallComment(commentJson, moreChildrenComment.depth.toFloat()).apply {
                    forEach { commentEntity ->
                        commentEntity.position = moreChildrenComment.position + commentCount*scale
                        commentCount += 1
                        Log.d(TAG, "load more scale ${commentEntity.position}")
                    }
                }
            }

            accum.apply { addAll(unmarshalledComments) }
        }
    }

    // TODO refactor and move the method into a comment entity method
    // TODO find a better way to unmarshall these objects and clean this up
    // won't be cleaned for a while because still decided how to format data and what is needed
    override suspend fun unmarshallComment(
        commentChild : JSONObject,
        commentPosition : Float
    ) : List<CommentEntity> {
        val commentPOJO = commentChild["data"] as JSONObject
        val commentList = ArrayList<CommentEntity>()

        coroutineScope {

            var deferredCommentData: Deferred<ParsedCommentData>? = null
            val commentEntity = gson.fromJson(commentPOJO.toString(), CommentEntity::class.java).apply {
                parentPostId = removeTypePrefix(commentPOJO["link_id"] as String)
                position = commentPosition

                commentPOJO["replies"]?.let { childJson ->
                    // try to parse the child json as nested replies
                    if (childJson.toString().isNotEmpty()) {
                        // parse the children of this comment
                        deferredCommentData = async {
                            parseComments(
                                postFullName = parentPostId,
                                response = childJson as JSONObject,
                                parentDepth = depth,
                                parentPosition = commentPosition
                            )
                        }
                    }
                }

                // converts fields that have already been unmarshalled by gson
                parent_id = removeTypePrefix(parent_id)
                author_flair_text?.let {
                    author_flair_text = Html.fromHtml(author_flair_text).toString()
                }

                // converts fields from json not in explicitly unmarshalled by gson
                userUpvoted = commentPOJO["likes"]?.run {
                    if (this as Boolean) 1 else -1
                } ?: 0

                commentPOJO["created"]?.let { created = formatDate(it as Double) }

                // get the gildings
                (commentPOJO["gildings"] as JSONObject?)?.let { gilding ->
                    gilding["gid_1"]?.let { platinum = it as Int }
                    gilding["gid_2"]?.let { gold = it as Int }
                    gilding["gid_3"]?.let { silver = it as Int }
                }

                // have to do this because Reddit has a decided this can be boolean or string
                try {
                    editedDate = formatDate(commentPOJO["edited"] as Double)
                } catch (e: Exception) { }
            }

            deferredCommentData?.let {
                it.await().let { parsedData ->
                    commentEntity.replyCount = parsedData.replyCount
                    commentList.addAll(parsedData.commentList)
                }
            }

            commentList.add(commentEntity)
        }

        return commentList
    }

    private fun formatDate(epochTime : Double) : String? {
        val commentCreated = Date(epochTime.toLong() * 1000)

        return if (currentYear != commentCreated.year) {
            // add year if the comment wasn't made in the current year
            "${commentCreated.year} ${formatter.format(commentCreated)}"
        } else {
            formatter.format(commentCreated)
        }
    }

    // endregion interface methods

    /**
     * Parse the response from the api and store the comments in the room db
     * @param response json string response
     * @param postFullName full name of post used as a key for the "after" value
     * @param parentDepth depth of the parent. Since posts start with a depth of 0, -1 is the
     * depth of the parent when calling from outside a recursive call
     * @param parentPosition positional value of the parent
     * @return : Parsed comment
     */
    private suspend fun parseComments(
        postFullName: String,
        response: JSONObject,
        parentDepth : Int = -1,
        parentPosition : Float = 0f
    ) : ParsedCommentData {
        val commentsData = (response["data"] as JSONObject)
        val listing = ListingEntity(postFullName, commentsData["after"]?.run { this as String })

        // get the list of children (comments) associated with the post
        val commentChildren = commentsData["children"] as JSONArray
        val commentList = ArrayList<CommentEntity>()

        // used for calculating the position of a comment
        val scale = 10f.pow(-(parentDepth + 1))
        var childCount = 1

        coroutineScope {
            commentChildren.forEach { commentChild ->
                val position = parentPosition + childCount*scale
                val commentJson = commentChild as JSONObject
                val childKind = commentJson["kind"] as String?

                if (childKind == "more") {
                    // means there is a "more object"
                    val deferredMore = async {
                        val moreData = (commentJson["data"] as JSONObject)
                        unmarshallMore(moreData, postFullName, position)
                    }
                    commentList.add(deferredMore.await())
                } else {
                    val deferredCommentList = async {
                        unmarshallComment(commentJson, position)
                    }
                    commentList.addAll(deferredCommentList.await())
                }

                childCount ++
            }
        }

        return ParsedCommentData(listing, commentList, commentChildren.size)
    }

    private fun unmarshallMore(
        moreJsonObject : JSONObject,
        postFullName : String,
        commentPosition : Float
    ) : CommentEntity {
        return CommentEntity().apply {
            id = moreJsonObject["name"] as String
            parentPostId = postFullName
            parent_id = moreJsonObject["parent_id"] as String
            created = CommentEntity.MORE_CREATED
            position = commentPosition
            depth = (moreJsonObject["depth"] as Long).toInt()

            val childrenLinks = moreJsonObject["children"] as JSONArray
            body_html = childrenLinks.toString()
            // reply count for "more" item will hold the number of comments to load
            replyCount = childrenLinks.size
        }
    }

    // removes the type associated with the comment, leaving only its id
    fun removeTypePrefix(fullName : String) : String {
        return if (fullName.length >= 4) {
            fullName.removeRange(0, 3)
        } else {
            ""
        }
    }

}