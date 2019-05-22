package com.relic.data.deserializer

import com.relic.data.PostRepository
import com.relic.data.entities.*
import com.relic.data.models.CommentModel
import com.relic.data.models.UserModel
import com.relic.exception.RelicException
import org.json.simple.JSONObject

/**
 * Decoupled from the repository package because deserializers should be responsible
 * for directly converting server response into entities -> repo shouldn't care how it's done,
 * just that the correct results are returned
 */
interface Contract {

    interface PostDeserializer {
        suspend fun parsePosts(
            response: String,
            postSource: PostRepository.PostSource,
            listingKey : String
        ) : ParsedPostsData

        suspend fun parsePost(response: String) : ParsedPostData
    }

    interface CommentDeserializer {
        suspend fun parseCommentsResponse(
            postFullName: String,
            response: String
        ) : ParsedCommentData

        suspend fun parseMoreCommentsResponse(
            moreChildrenComment: CommentModel,
            response : String
        ) : List<CommentEntity>

        suspend fun unmarshallComment(
            commentChild : JSONObject,
            commentPosition : Float
        ) : List<CommentEntity>
    }

    interface UserDeserializer {
        suspend fun parseUser(userResponse: String, trophiesResponse : String) : UserModel

        suspend fun parseUsername(response: String) : String
    }

    interface AccountDeserializer {
        suspend fun parseAccount(accountResponse : String) : AccountEntity
    }

    interface SubDeserializer {
        suspend fun parseSubredditResponse(response: String): SubredditEntity
        suspend fun parseSubredditsResponse(response: String): ParsedSubsData
        suspend fun parseSearchSubsResponse(response: String): List<String>
    }

}

data class ParsedPostData(
    val postSourceEntity:PostSourceEntity,
    val postEntity : PostEntity
)

data class ParsedPostsData(
    val postSourceEntities:List<PostSourceEntity>,
    val postEntities : List<PostEntity>,
    val commentEntities : List<CommentEntity>,
    val listingEntity: ListingEntity
)

data class ParsedCommentData(
    val listingEntity : ListingEntity,
    val commentList : List<CommentEntity>,
    val replyCount : Int
)

data class ParsedSubsData(
    val subsList : List<SubredditEntity>,
    val after : String?
)

class RelicParseException(message : String, cause : Throwable) : RelicException(message, cause)