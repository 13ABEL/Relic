package com.relic.presentation.Frontpage;

import android.arch.lifecycle.ViewModel;

import com.relic.data.Authenticator;
import com.relic.data.PostRepository;

public class FrontpageVM extends ViewModel implements FrontpageContract.VM {
  private String defaultSub = "/r/frontpage/hot.json";
  PostRepository postRepo;
  Authenticator authenticator;

  public void init(PostRepository postRepository, Authenticator auth) {
    this.postRepo = postRepository;
    this.authenticator = auth;

    // get the posts for the frontpage as default
    postRepo.getPosts(defaultSub);
  }

}
