package com.relic.data

import android.arch.lifecycle.LiveData
import com.relic.data.models.AccountModel
import com.relic.data.models.UserModel
import com.relic.data.repository.RepoError

interface UserRepository {
    @Throws(RepoError::class)
    suspend fun retrieveUser(username : String) : UserModel?

    @Throws(RepoError::class)
    suspend fun retrieveSelf() : String?

    // region user authenticated functions

    /**
     * adds an account to set of authenticated account
     */
    suspend fun addAuthenticatedAccount(username : String)

    /**
     * sets an account from the list of authenticated account as the current one
     */
    suspend fun setCurrentAccount(username : String)

    /**
     * sets an account from the list of authenticated account as the current one
     */
    suspend fun getAccounts() : LiveData<List<AccountModel>>

    /**
     * gets the current account in use
     */
    fun getCurrentAccount() : String?

    // endregion user authenticated functions

    @Throws(RepoError::class)
    suspend fun retrieveAccount(name : String)
}

sealed class UserRepoException : Exception() {
    object UserAlreadyAuthenticated : UserRepoException()

    object UserNotAuthenticated : UserRepoException()
}