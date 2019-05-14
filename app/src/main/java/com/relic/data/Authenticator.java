package com.relic.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.relic.R;
import com.relic.network.VolleyQueue;
import com.relic.presentation.callbacks.AuthenticationCallback;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton instance of the authenticator because we should be able to
 */
public class Authenticator {
  private final String TAG = "AUTHENTICATOR";
  private final String BASE = "https://www.reddit.com/api/v1/authorize.compact?";
  private final String ACCESS_TOKEN_URI = "https://www.reddit.com/api/v1/access_token";
  private final String REDIRECT_URI = "https://github.com/13ABEL/Relic";
  private final String DURATION="permanent";

  private String preference;
    private String redirectCode;

    // keys for shared preferences
    private String KEY_USERNAME = "PREF_USERNAME";
  private String tokenKey;
  private String refreshTokenKey;

  private String responseType = "code";
  private String state = "random0101"; // any random value
  private String scope = "identity account mysubreddits edit flair history modconfig modflair modlog modposts modwiki" +
      " mysubreddits privatemessages read report save submit subscribe vote wikiedit wikiread";

  private Context appContext;
  private RequestQueue requestQueue;
  private Date lastRefresh;



  /**
   * Private constructor to initialize the single instance of the Authenticator
   * @param applicationContext application context
   */
  public Authenticator(Context applicationContext) {
    appContext = applicationContext;
    requestQueue = VolleyQueue.get(applicationContext);

    Resources resources = appContext.getResources();
    // retrieve the strings from res
    preference = resources.getString(R.string.AUTH_PREF);
    tokenKey = resources.getString(R.string.TOKEN_KEY);
    refreshTokenKey = resources.getString(R.string.REFRESH_TOKEN_KEY);
    redirectCode = resources.getString(R.string.REDIRECT_CODE);

    if (isAuthenticated()) {
      // refresh the auth token
      // move the date change to the refresh method
      Log.d(TAG, "Current date/time: " + Calendar.getInstance().getTime());
      lastRefresh = Calendar.getInstance().getTime();
    }
  }

  public String getUrl() {
    return BASE + "client_id=" + appContext.getString(R.string.client_id)
        + "&response_type=" + responseType
        + "&state=" + state
        + "&redirect_uri=" + REDIRECT_URI
        + "&duration=" + DURATION
        + "&scope=" + scope;
  }


  public String getRedirect() {
    return this.REDIRECT_URI;
  }


  /**
   * Callback used to parse the url after the user has authenticated through the reddit auth page.
   * Retrieves the code value and uses it to obtain the real auth token
   * @param redirectUrl url with params to parse
   */
  public void retrieveAccessToken(String redirectUrl, AuthenticationCallback callback) {
    String queryStrings = redirectUrl.substring(REDIRECT_URI.length() + 1);
    String[] queryPairs = queryStrings.split("&");

    // parses the redirect to get the access token needed to retrieve the access token
    final Map<String, String> queryMap = new HashMap<>();
    for (String queryPair : queryPairs) {
      String[] mapping = queryPair.split("=");
      queryMap.put(mapping[0], mapping[1]);
    }
    // stores the redirect "code" in shared preferences for easy access
    Log.d(TAG, queryMap.keySet().toString() + " " + queryMap.get(redirectCode));
    appContext.getSharedPreferences(preference, Context.MODE_PRIVATE).edit()
        .putString(redirectCode, queryMap.get(redirectCode)).apply();

    // get the access and refresh token
    requestQueue.add(new RedditGetTokenRequest(Request.Method.POST, ACCESS_TOKEN_URI,
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            Log.d(TAG, response);
            saveReturn(response, callback);
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "Error retrieving access token through reddit " + error.toString());
          }
        })
    );
  }


  /**
   * Refreshes the current access token using the refresh token by getting a new one
   */
  public void refreshToken(AuthenticationCallback callback) {
    requestQueue.add(new RedditGetRefreshRequest(Request.Method.POST, ACCESS_TOKEN_URI,
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Token refreshed" + response);
            saveReturn(response, callback);

            // set time of refresh
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "Token failed to refresh = " + error.toString());
          }
        })
    );
  }

  public String getTokenKey() {
    // retrieve the auth token shared preferences
    String authKey = appContext.getResources().getString(R.string.AUTH_PREF);
    String tokenKey = appContext.getResources().getString(R.string.TOKEN_KEY);
    return appContext.getSharedPreferences(authKey, Context.MODE_PRIVATE)
            .getString(tokenKey, "DEFAULT");
  }

  /**
   * checks if the user is currently signed in by checking shared preferences
   * @return whether the user is signed in
   */
  public boolean isAuthenticated() {
    return appContext.getSharedPreferences(preference, Context.MODE_PRIVATE)
        .contains(tokenKey);
  }

    public void initializeUser(String username) {
        // stores the current user in shared preferences
        SharedPreferences.Editor prefEditor = appContext
            .getSharedPreferences(KEY_USERNAME, Context.MODE_PRIVATE).edit();

        prefEditor.putString(KEY_USERNAME, username).apply();
    }

    public String getUser() {
        // stores the current user in shared preferences
        SharedPreferences prefEditor = appContext
            .getSharedPreferences(KEY_USERNAME, Context.MODE_PRIVATE);

        return prefEditor.getString(KEY_USERNAME, null);
    }

  /**
   * parses the successful auth response to store the oauth and refresh token in the shared
   * preferences. Then refreshes the token to get the permanent token
   * @param response
   */
  private void saveReturn(String response, AuthenticationCallback callback) {
    JSONParser parser = new JSONParser();
    try {
      JSONObject data = (JSONObject) parser.parse(response);
      Log.d(TAG, "" + data.get("scope").toString());

      // stores the token in shared preferences
      SharedPreferences.Editor prefEditor = appContext
          .getSharedPreferences("auth", Context.MODE_PRIVATE).edit();

      // checks if there is an refresh token to be stored
      if (data.containsKey(refreshTokenKey)) {
          prefEditor.putString(refreshTokenKey, (String) data.get(refreshTokenKey)).apply();
      }

      prefEditor.putString(tokenKey, (String) data.get(tokenKey)).apply();
      Log.d(TAG, "token saved! " + (String) data.get(tokenKey));

      callback.onAuthenticated();

    } catch (ParseException e) {
        // TODO remove toast code. It has no business being here
      Toast.makeText(appContext, "yikes", Toast.LENGTH_SHORT).show();
    }
  }

  class RedditGetTokenRequest extends StringRequest {
    private String code;

    private RedditGetTokenRequest(int method, String url, Response.Listener<String> listener,
                                 Response.ErrorListener errorListener) {
      super(method, url, listener, errorListener);

      code = appContext.getSharedPreferences(preference, Context.MODE_PRIVATE)
          .getString(redirectCode, "DEFAULT");
    }

    // override headers to add custom credentials in client_secret:redirect_code format
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
      // create a new header map and add the right headers to it
      Map<String, String> headers = new HashMap<>();

      // generate encoded credential string with client id and code from redirect
      String credentials = appContext.getString(R.string.client_id) + ":" + code;
      String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
      headers.put("Authorization", auth);

      return headers;
    }

    @Override
    public Map<String, String> getParams() throws AuthFailureError {
      Map<String, String> params = new HashMap<>();

      params.put("grant_type", "authorization_code");
      params.put("code", code);
      params.put("redirect_uri", REDIRECT_URI);
      return params;
    }
  }


  class RedditGetRefreshRequest extends StringRequest{
    private RedditGetRefreshRequest(int method, String url, Response.Listener<String> listener,
                                  Response.ErrorListener errorListener) {
      super(method, url, listener, errorListener);
    }

    // override headers to add custom credentials in client_secret:redirect_code format
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
      // create a new header map and add the right headers to it
      Map<String, String> headers = new HashMap<>();

      String code = appContext.getSharedPreferences(preference, Context.MODE_PRIVATE)
          .getString(redirectCode, "DEFAULT");
      // generate encoded credential string with client id and code from redirect
      String credentials = appContext.getString(R.string.client_id) + ":" + code;
      String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
      headers.put("Authorization", auth);

      return headers;
    }

    public Map<String, String> getParams() throws AuthFailureError {
      Map<String, String> params = new HashMap<>();

      String refreshToken = appContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
          .getString(refreshTokenKey, "DEFAULT");

      params.put("grant_type", refreshTokenKey);
      params.put("refresh_token", refreshToken);

      return params;
    }
  }

}
