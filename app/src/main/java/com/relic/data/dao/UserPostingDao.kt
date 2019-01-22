package com.relic.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.relic.data.models.PostModel

@Dao
abstract class UserPostingDao {
// for user specific actions

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE userSubmittedPosition >= 0 ORDER BY userSubmittedPosition ASC")
    abstract fun getUserPosts(): LiveData<List<PostModel>>

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE userSavedPosition >= 0 ORDER BY userSavedPosition ASC")
    abstract fun getUserSavedPosts(): LiveData<List<PostModel>>

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE userUpvotedPosition >= 0 ORDER BY userUpvotedPosition ASC")
    abstract fun getUserUpvotedPosts(): LiveData<List<PostModel>>

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE  userDownvotedPosition >= 0 ORDER BY userDownvotedPosition ASC")
    abstract fun getUserDownvotedPosts(): LiveData<List<PostModel>>

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE userGildedPosition >= 0 ORDER BY userGildedPosition ASC")
    abstract fun getUserGilded(): LiveData<List<PostModel>>

    @Query("SELECT * FROM PostEntity " +
        "LEFT JOIN PostSourceEntity ON PostEntity.id = PostSourceEntity.sourceId " +
        "WHERE userHiddenPosition >= 0 ORDER BY userHiddenPosition ASC")
    abstract fun getUserHidden(): LiveData<List<PostModel>>
}