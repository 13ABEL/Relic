package com.relic.presentation.displaysub;

import android.arch.lifecycle.LiveData;

import com.relic.data.PostRepository;
import com.relic.data.SubRepository;
import com.relic.data.models.PostModel;
import com.relic.data.models.SubredditModel;

import java.util.List;

public class DisplaySubContract {
  public interface ViewModel {
    void init(String subredditName, SubRepository subRepo, PostRepository postRepo);

    /**
     * Used to check if the viewmodel has already been initialized
     * @return
     */
    boolean isInitialized();

    LiveData<SubredditModel> getSubModel();

    LiveData<List<PostModel>> getPosts();

    String getSubName();

    void retrieveMorePosts(boolean resetPosts);

    LiveData<Boolean> subscribeToSub();
  }
}
