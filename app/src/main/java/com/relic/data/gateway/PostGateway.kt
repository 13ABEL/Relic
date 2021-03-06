package com.relic.data.gateway

interface PostGateway {
    suspend fun voteOnPost(fullname: String, voteStatus: Int)

    suspend fun savePost(fullname: String, save: Boolean)

    suspend fun comment(fullname: String, comment: String)

    suspend fun gildPost(fullname: String, gild: Boolean)

    suspend fun reportPosts(fullname: String, report: Boolean)

    suspend fun visitPost(postFullname: String)
}
