package com.relic.presentation.displaypost;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.relic.R;
import com.relic.data.CommentRepositoryImpl;
import com.relic.data.PostRepositoryImpl;
import com.relic.data.models.CommentModel;
import com.relic.data.models.PostModel;
import com.relic.databinding.DisplayPostBinding;
import com.relic.presentation.adapter.CommentAdapter;
import com.relic.presentation.callbacks.PostLoadCallback;

import java.util.List;

public class DisplayPostView extends Fragment {
  private final String TAG = "DISPLAYPOST_VIEW";
  private DisplayPostContract.ViewModel displayPostVM;
  private DisplayPostBinding displayPostBinding;
  private CommentAdapter commentAdapter;

  private String postFullname;
  private String subreddit;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }


  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    displayPostBinding = DataBindingUtil
        .inflate(inflater, R.layout.display_post, container, false);

    try {
      // parse the full name of the post to be displayed
      postFullname = getArguments().getString("full_name");
      subreddit = getArguments().getString("subreddit");
      Log.d(TAG, "Post fullname : " + postFullname);
    }
    catch (Exception e) {
      Toast.makeText(getContext(), "Fragment not loaded properly!", Toast.LENGTH_SHORT).show();
    }

    commentAdapter = new CommentAdapter();
    displayPostBinding.displayCommentsRecyclerview.setAdapter(commentAdapter);
    displayPostBinding.displayCommentsRecyclerview.setNestedScrollingEnabled(false);

    return displayPostBinding.getRoot();
  }


  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // create the VM and initialize it with injected dependencies
    displayPostVM = ViewModelProviders.of(this).get(DisplayPostVM.class);
    displayPostVM.init(new PostRepositoryImpl(getContext()),
        new CommentRepositoryImpl(getContext()), subreddit, postFullname);

    subscribeToVM();
  }


  /**
   * subscribes the view to the data exposed by the viewmodel
   */
  private void subscribeToVM() {
    // Observe the post exposed by the VM
    displayPostVM.getPost().observe(this, new Observer<PostModel>() {
      @Override
      public void onChanged(@Nullable PostModel postModel) {
        if (postModel != null) {
          displayPostBinding.setPostItem(postModel);
        }
      }
    });

    // Observe the list of comments exposed by the VM
    displayPostVM.getCommentList().observe(this, new Observer<List<CommentModel>>() {
      @Override
      public void onChanged(@Nullable List<CommentModel> commentModels) {
        // notify the adapter and set the new list
        if (commentModels != null) {
          commentAdapter.setComments(commentModels);
          Log.d(TAG, "Comments " + commentModels.size());
        }
      }
    });
  }




}