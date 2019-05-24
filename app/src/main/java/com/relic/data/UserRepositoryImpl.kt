package com.relic.data

import android.arch.lifecycle.LiveData
import android.content.Context
import android.util.Log
import com.relic.data.deserializer.*
import com.relic.data.models.AccountModel
import com.relic.data.models.UserModel
import com.relic.data.repository.RepoConstants.ENDPOINT
import com.relic.network.NetworkRequestManager
import com.relic.network.request.RelicOAuthRequest
import kotlinx.coroutines.*

class UserRepositoryImpl (
    private val appContext: Context,
    private val requestManager: NetworkRequestManager
): UserRepository {
    private val TAG = "USER_REPO"

    private val KEY_ACCOUNTS_DATA = "PREF_ACCOUNTS_DATA"
    private val KEY_CURR_ACCOUNT = "PREF_CURR_ACCOUNT"

    private val appDB = ApplicationDB.getDatabase(appContext)
    private val accountDao = appDB.accountDao

    private val userDeserializer : Contract.UserDeserializer = UserDeserializerImpl()
    private val accountDeserializer : Contract.AccountDeserializer = AccountDeserializerImpl()

    override suspend fun retrieveUsername(): String? {
        val selfEndpoint = "${ENDPOINT}api/v1/me"
        var username : String? = null

        withContext(Dispatchers.IO) {
            try {
                // create the new request and submit it
                val response = requestManager.processRequest(
                    method = RelicOAuthRequest.GET,
                    url = selfEndpoint
                )

                username = userDeserializer.parseUsername(response)
            } catch (e: Exception) {
                throw DomainTransfer.handleException("retrieve username", e) ?: e
            }
        }

        return username
    }

    override suspend fun retrieveUser(username: String): UserModel? {
        val userEndpoint = "${ENDPOINT}user/$username/about"
        val trophiesEndpoint = "${ENDPOINT}api/v1/user/$username/trophies"

        var userModel : UserModel? = null

        try {
            val userResponse = requestManager.processRequest(
                method = RelicOAuthRequest.GET,
                url = userEndpoint
            )

            val trophiesResponse = requestManager.processRequest(
                method = RelicOAuthRequest.GET,
                url = trophiesEndpoint
            )

            Log.d(TAG, "more posts $userResponse")
            Log.d(TAG, "trophies $trophiesResponse")

            userModel = userDeserializer.parseUser(userResponse, trophiesResponse)
        } catch (e: Exception) {
            throw DomainTransfer.handleException("retrieve user", e) ?: e
        }

        return userModel
    }

    override suspend fun retrieveCurrentUser(): UserModel? {
        val name = appContext.getSharedPreferences(KEY_ACCOUNTS_DATA, Context.MODE_PRIVATE)
            .getString(KEY_CURR_ACCOUNT, null)

        return retrieveUser(name)
    }

    override suspend fun setCurrentAccount(username: String) {
        appContext.getSharedPreferences(KEY_ACCOUNTS_DATA, Context.MODE_PRIVATE).let { sp ->
            sp.edit().putString(KEY_CURR_ACCOUNT, username)?.apply()
        }
    }

    override fun getAccounts(): LiveData<List<AccountModel>> {
        return accountDao.getAccounts()
    }

    override suspend fun retrieveAccount(name : String) {
        val url = "$ENDPOINT/api/v1/me/prefs"
        try {
            val response = requestManager.processRequest(RelicOAuthRequest.GET, url)
            Log.d(TAG, response)

            accountDeserializer.parseAccount(response).let { account ->
                // need to manually specify name here
                account.name = name
                withContext(Dispatchers.IO) {
                    accountDao.insertAccount(account)
                }
            }
        }
        catch (e : Exception){
            throw DomainTransfer.handleException("retrieve account", e) ?: e
        }
    }


}