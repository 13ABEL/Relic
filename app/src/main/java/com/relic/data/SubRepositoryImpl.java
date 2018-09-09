package com.relic.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.relic.R;
import com.relic.data.Request.RedditOauthRequest;
import com.relic.data.entities.SubredditEntity;
import com.relic.data.gateway.SubGateway;
import com.relic.data.gateway.SubGatewayImpl;
import com.relic.data.models.SubredditModel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SubRepositoryImpl implements SubRepository {
  private final String ENDPOINT = "https://oauth.reddit.com/";
  private final String KEY = "SUBSCRIBED";
  private final String userAgent = "android:com.relic.Relic (by /u/boiledbuns)";
  private final String TAG = "SUB_REPO";

  private ApplicationDB appDb;
  private Context context;
  private RequestQueue volleyQueue;
  private String authToken;
  private JSONParser parser;
  private Gson gson;

  private MutableLiveData <Boolean> allSubscribedSubsLoaded;

  public SubRepositoryImpl(Context context) {
    Authenticator auth = new Authenticator(context);
    this.context = context;
    volleyQueue = VolleyAccessor.getInstance(context).getRequestQueue();
    parser = new JSONParser();
    gson = new GsonBuilder().create();

    // retrieve the auth token shared preferences
    authToken = checkToken();
    appDb = ApplicationDB.getDatabase(context);
    initializeLivedata();
  }

  private String checkToken() {
    // retrieve the auth token shared preferences
    String authKey = context.getResources().getString(R.string.AUTH_PREF);
    String tokenKey = context.getResources().getString(R.string.TOKEN_KEY);
    return context.getSharedPreferences(authKey, Context.MODE_PRIVATE)
        .getString(tokenKey, "DEFAULT");
  }

  /**
   * Initializes all the livedata that this repository exposes
   */
  private void initializeLivedata() {
    allSubscribedSubsLoaded = new MutableLiveData<>();
    allSubscribedSubsLoaded.setValue(true);
  }


  @Override
  public LiveData<Boolean> getAllSubscribedSubsLoaded() {
    return allSubscribedSubsLoaded;
  }

  /**
   * Returns the list of subscribed subs from the sqLite instance
   * @return list of subscribed subs in the database as livedata
   */
  @Override
  public LiveData<List<SubredditModel>> getSubscribedSubs() {
    return appDb.getSubredditDao().getAllSubscribed();
  }

  public void retrieveAllSubscribedSubs() {
    Log.d(TAG, "retrieving all new subscrubed subs");

    // since refreshing, set all subs loaded to reflect that not all subs are loaded
    allSubscribedSubsLoaded.setValue(false);
    // delete all locally stored subs
    new deleteSubscribedSubsTask().execute(appDb);

    retrieveMoreSubscribedSubs(null);
  }

  public void retrieveMoreSubscribedSubs(String after) {
//    String ending = "";
//    if (after == null) {
//      // if refreshing, set all subs loaded to reflect that not all subs are loaded
//      allSubscribedSubsLoaded.setValue(false);
//      // delete all locally stored subs
//      new deleteSubscribedSubsTask().execute(appDb);
//    }
//    else {
//      // change the query string if fetching all subscribed subreddits from scratch
//      ending = "?limit=50&after=" + after;
//    }

    String ending = "subreddits/mine/subscriber?limit=30&after=" + after;
    // create the new request to reddit servers and store the data in persistence layer
    volleyQueue.add(new RedditOauthRequest(
        Request.Method.GET, ENDPOINT + ending,
        response -> {
          try {
            String newAfter = (String) ((JSONObject) ((JSONObject) parser.parse(response)).get("data")).get("after");
            Log.d(TAG, "after = " + newAfter);
            // insert the subs and listing into the room instance
            InsertSubsTask insertSubsTask = new InsertSubsTask();
            insertSubsTask.subRepo = this;
            insertSubsTask.subDB = appDb;
            insertSubsTask.subs = parseSubreddits(response);
            insertSubsTask.after = newAfter;
            insertSubsTask.allSubbedSubsLoaded = allSubscribedSubsLoaded;
            insertSubsTask.execute();
          } catch (ParseException e) {
            Log.e(TAG, "Error parsing the response: " + e.toString());
          }
        },
        (VolleyError error) -> {
          Log.d(TAG, "Error retrieving the response for " + after + " : " + error.networkResponse);
          Log.d(TAG, "Trying again");

          // try to retrieve the subs again
          retrieveMoreSubscribedSubs(after);
        }, checkToken()));
  }

//  /**
//   * Parses the "after" value from the listing to get the next listing
//   * @param response JSON formatted response
//   * @return a listing entity to hold the after value
//   * @throws ParseException potential GSON exception when unmarshalling object
//   */
//  private ListingEntity parseAfterValue(String response) throws ParseException {
//    JSONObject data = (JSONObject) ((JSONObject) parser.parse(response)).get("data");
//    Log.d(TAG, "banner url  = " + currentSub.get("banner_background_image") + " " + currentSub.get("banner_img"));
//    // create a new listing to ensure that the db has an "after" value for checking if we need to
//    // fetch more values or not
//    return new ListingEntity(TAG, (String) data.get("after"));
//  }

  /**
   * Parses a "listing" for subreddits into a list of Subreddit
   * @param response JSON formatted listing for the subreddits to be parsed
   * @return list of subreddit entities parsed from the string
   * @throws ParseException potential GSON exception when unmarshalling object
   */
  private List<SubredditEntity> parseSubreddits(String response) throws ParseException {
    //Log.d(TAG, response);
    JSONObject data = (JSONObject) ((JSONObject) parser.parse(response)).get("data");
    List <SubredditEntity> subscribed = new ArrayList<>();

    // get all the subs that the user is subscribed to
    JSONArray subs = (JSONArray) data.get("children");
    for (Object sub : subs) {
      JSONObject currentSub = (JSONObject) ((JSONObject) sub).get("data");
      //Log.d(TAG, "keys = " + currentSub.keySet());
      // Log.d(TAG, "banner url  = " + currentSub.get("banner_background_image") + " " + currentSub.get("banner_img"));
      //Log.d(TAG, currentSub.get("display_name") + "banner url  = " + currentSub.get("community_icon") + " " + currentSub.get("icon_img"));
      subscribed.add(gson.fromJson(currentSub.toJSONString(), SubredditEntity.class));
    }

    //Log.d(TAG, subscribed.toString());
    return subscribed;
  }


  public LiveData<SubredditModel> getSingleSub(String subName) {
    return appDb.getSubredditDao().getSub(subName);
  }

  @Override
  public void retrieveSingleSub(String subName) {
    volleyQueue.add(new RedditOauthRequest(Request.Method.GET, ENDPOINT + "r/" + subName  +"/about",
        (String response) -> {
          Log.d(TAG, response);

          try {
            // parse the response and add it to an arraylist to be inserted in the db
            JSONObject subredditObject = (JSONObject) ((JSONObject) parser.parse(response)).get("data");
            SubredditEntity subreddit = gson.fromJson(subredditObject.toJSONString(), SubredditEntity.class);

            // create a new task to insert the subreddits on parse success
            new InsertSubTask().execute(appDb, subreddit);

          } catch (ParseException e) {
            e.printStackTrace();
          }

        }, (VolleyError e) -> {
      Log.d(TAG, "There was an error retrieving the response from the server " + e.getMessage());
    }, checkToken()));
  }


  static class InsertSubsTask extends AsyncTask <String, Integer, Integer> {
    ApplicationDB subDB;
    SubRepositoryImpl subRepo;
    List<SubredditEntity> subs;
    //ListingEntity listingEntity;
    String after;
    MutableLiveData <Boolean> allSubbedSubsLoaded;

    @Override
    protected Integer doInBackground(String... Strings) {
      subDB.getSubredditDao().insertAll(subs);
      if (after != null) {
//        // checks the after value of the listing for the current subs and update the local listing
//        subDB.getListingDAO().insertListing(listingEntity);
        // retrieve more subs without refreshing if the string is null
        subRepo.retrieveMoreSubscribedSubs(after);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Integer integer) {
      // if no after value, set value to true to reflect all subs have been loaded
      if (after == null) {
        allSubbedSubsLoaded.setValue(true);
      }
    }
  }


  @Override
  public void searchSubreddits(MutableLiveData<List<String>> liveResults, String query) {
    String end = ENDPOINT + "api/search_subreddits?query=" + query;
      volleyQueue.add(new RedditOauthRequest(Request.Method.POST, end,
          response -> {
            Log.d(TAG, response);

//            List<String> livedataResults = liveResults.getValue();
//            livedataResults.addAll(parseSearchedSubs(response));
//            liveResults.setValue(livedataResults);

            liveResults.setValue(parseSearchedSubs(response));
          },
          error -> {
            Log.d(TAG, "error retrieving this search results");
          }, checkToken()));
  }

  /**
   * Parses the api response to obtain the list of subreddit names
   * @param response search_subreddits api response
   */
  private List<String> parseSearchedSubs(String response) {
    List <String> parsedMatches = new ArrayList<>();

    try {
      JSONArray subreddits = (JSONArray) ((JSONObject) parser.parse(response)).get("subreddits");
      Iterator subIterator = subreddits.iterator();

      while (subIterator.hasNext()) {
        parsedMatches.add((String) ((JSONObject) subIterator.next()).get("name"));
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return parsedMatches;
  }


  @Override
  public SubGateway getSubGateway() {
    return new SubGatewayImpl(context);
  }


  static class InsertSubTask extends AsyncTask <Object, Integer, Integer> {
    @Override
    protected Integer doInBackground(Object... objects) {
      ApplicationDB applicationDB = (ApplicationDB) objects[0];
      applicationDB.getSubredditDao().insert((SubredditEntity) objects[1]);
      return null;
    }
  }

  static class deleteSubscribedSubsTask extends  AsyncTask<ApplicationDB, Integer, Integer>{
    @Override
    protected Integer doInBackground(ApplicationDB... appDBs) {
      ApplicationDB appdb = appDBs[0];
      appdb.getSubredditDao().deleteAllSubscribed();
      return null;
    }
  }
}
