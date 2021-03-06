package com.relic.network

import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.relic.data.Auth
import com.relic.data.repository.AuthException
import com.relic.network.request.RelicOAuthRequest
import com.relic.presentation.callbacks.AuthenticationCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Abstraction for all network requests
 */
@Singleton
class NetworkRequestManager @Inject constructor(
    private val volleyQueue: RequestQueue
) {
    private lateinit var authManager: Auth

    @Throws(VolleyError::class)
    suspend fun processUnauthenticatedRequest(
        method: Int,
        url: String,
        headers: MutableMap<String, String>? = null,
        data: MutableMap<String, String>? = null
    ): String {
        return suspendCoroutine { cont ->
            val request = RelicOAuthRequest(
                method = method,
                url = url,
                listener = Response.Listener { response: String ->
                    cont.resumeWith(Result.success(response))
                },
                errorListener = Response.ErrorListener { e: VolleyError ->
                    cont.resumeWithException(e)
                },
                headers = headers,
                data = data
            )

            volleyQueue.add(request)
        }
    }

    /**
     * note that token should 99% be left empty (checked here) when calling process request
     * token field should only be filled in when processing a request for the first time
     * when a token is unavailable
     */
    @Throws(VolleyError::class)
    suspend fun processRequest(
        method: Int,
        url: String,
        authToken: String? = null,
        headers: MutableMap<String, String>? = null,
        data: MutableMap<String, String>? = null,
        retryAuthAllowed: Boolean = true
    ): String {
        val token = authToken ?: authManager.getToken()

        Timber.d("endpoint: %s ", url)

        return suspendCoroutine { cont ->
            if (authToken == null && !authManager.isAuthenticated()) throw AuthException("user not logged in", null)
            val request = RelicOAuthRequest(
                method, url,
                Response.Listener { response: String ->
                    Timber.d("endpoint: %s \n response:  %s", url, response)
                    cont.resumeWith(Result.success(response))
                },
                Response.ErrorListener { e: VolleyError ->
                    // check if the failure is the result of an unauthenticated token
                    // try to refresh token and try request
                    if (e is AuthFailureError && retryAuthAllowed) {
                        // TODO global scope is ugly here, should change in future
                        GlobalScope.launch { handleAuthError(method, url, headers, data, cont) }
                    } else {
                        cont.resumeWithException(e)
                    }
                },
                token,
                headers,
                data
            )
            volleyQueue.add(request)
        }
    }

    // if a request fails because the token has expired, this function will
    // refresh the token and retry the request
    private suspend fun handleAuthError(
        method: Int,
        url: String,
        headers: MutableMap<String, String>? = null,
        data: MutableMap<String, String>? = null,
        cont: Continuation<String>
    ) {
        Timber.d("refresh token")
        authManager.refreshToken(callback = AuthenticationCallback {
            GlobalScope.launch {
                try {
                    val response = processRequest(method, url, null, headers, data, false)
                    cont.resumeWith(Result.success(response))
                } catch (e: Exception) {
                    cont.resumeWith(Result.failure(e))
                }
            }
        })
    }

    fun setAuthManager(auth: Auth) {
        authManager = auth
    }
}