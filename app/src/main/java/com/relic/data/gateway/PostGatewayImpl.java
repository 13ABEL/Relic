package com.relic.data.gateway;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.relic.R;
import com.relic.data.ApplicationDB;
import com.relic.data.Request.RedditOauthRequest;
import com.relic.data.VolleyAccessor;

public class PostGatewayImpl implements  PostGateway {
  private final String ENDPOINT = "https://oauth.reddit.com/";
  public static String TAG = "SUB_GATEWAY";
  private String authToken;

  private ApplicationDB appDb;

  RequestQueue requestQueue;


  public PostGatewayImpl(Context context) {
    appDb = ApplicationDB.getDatabase(context);
    // Get the key values needed to get the actual authtoken from shared preferences
    String authKey = context.getString(R.string.AUTH_PREF);
    String tokenKey = context.getString(R.string.TOKEN_KEY);

    // retrieve the authtoken for use
    authToken = context.getSharedPreferences(authKey, Context.MODE_PRIVATE)
        .getString(tokenKey, "DEFAULT");

    requestQueue = VolleyAccessor.getInstance(context).getRequestQueue();
  }

  @Override
  public LiveData<Boolean> voteOnPost(String fullname, int voteStatus) {
    // generate the voting endpoint
    String ending = ENDPOINT + "api/vote?id=" + fullname;
    ending += "&dir=" + voteStatus;

    MutableLiveData<Boolean> success = new MutableLiveData<>();

    requestQueue.add(new RedditOauthRequest(Request.Method.POST, ending,
        (String response) -> {
          success.setValue(true);
          // update the locally stored instance
        },
        (VolleyError error) -> {
          Log.d(TAG, "Sorry, there was an error voting on the post " + fullname + " to " + voteStatus);
        }, authToken));

    return success;
  }


  @Override
  public LiveData<Boolean> savePost(String fullname, boolean saved) {
    MutableLiveData<Boolean> success = new MutableLiveData<>();


    return null;
  }

  @Override
  public LiveData<Boolean> comment(String fullname, String comment) {
    MutableLiveData<Boolean> success = new MutableLiveData<>();

    return null;
  }

  @Override
  public LiveData<Boolean> gildPost(String fullname, boolean gild) {
    MutableLiveData<Boolean> success = new MutableLiveData<>();

    return null;
  }

  @Override
  public LiveData<Boolean> reportPosts(String fullname, boolean report) {
    MutableLiveData<Boolean> success = new MutableLiveData<>();

    return null;
  }


}