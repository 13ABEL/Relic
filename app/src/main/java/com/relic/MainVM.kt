package com.relic

import android.arch.lifecycle.*
import android.util.Log
import com.relic.data.auth.AuthImpl
import com.relic.data.UserRepository
import com.relic.data.models.AccountModel
import com.relic.data.models.UserModel
import com.relic.presentation.base.RelicViewModel
import com.relic.presentation.callbacks.AuthenticationCallback
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MainVM(
    private val auth : AuthImpl,
    private val userRepo : UserRepository
) : RelicViewModel(), MainContract.VM, CoroutineScope {

    class Factory @Inject constructor(
        private val auth : AuthImpl,
        private val userRepo : UserRepository
    ) {
        fun create () : MainVM {
            return MainVM(auth, userRepo)
        }
    }

    private val _accountsLiveData = MediatorLiveData<List<AccountModel>>()
    private val _userLiveData = MediatorLiveData<UserModel>()

    val accountsLiveData : LiveData<List<AccountModel>> = _accountsLiveData
    val userLiveData : LiveData<UserModel> = _userLiveData

    init {
        launch(Dispatchers.Main) {
            auth.refreshToken(AuthenticationCallback {
                Log.d(TAG, "Token refreshed")
                retrieveUser()
            })
        }

        _accountsLiveData.addSource(userRepo.getAccounts()) { accounts ->
            _accountsLiveData.postValue(accounts)
        }
    }

    override fun onAccountSelected(name : String?) {
        launch(Dispatchers.Main) {
            // update the current account so we can retrieve the user associated with it
            userRepo.setCurrentAccount(name!!)
            // since we're switching the user, need to refresh the auth token
            auth.refreshToken(AuthenticationCallback {
                retrieveUser()
            })
        }
    }

    private fun retrieveUser() {
        launch(Dispatchers.Main) {
            // need to retrieve current user (to get the username) before retrieving the account
            userRepo.retrieveCurrentUser()?.let { user ->
                Log.d(TAG, "user $user")
                _userLiveData.postValue(user)

                userRepo.retrieveAccount(user.name)
            }
        }
    }

    override fun handleException(context: CoroutineContext, e: Throwable) {}
}