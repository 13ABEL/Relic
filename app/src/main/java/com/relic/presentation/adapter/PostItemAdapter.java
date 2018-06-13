package com.relic.presentation.adapter;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.relic.R;
import com.relic.data.models.PostModel;
import com.relic.databinding.PostItemBinding;
import com.relic.presentation.displaypost.DisplayPostView;

import java.util.ArrayList;
import java.util.List;

public class PostItemAdapter extends RecyclerView.Adapter<PostItemAdapter.PostItemVH>{
  private final String TAG = "POST_ADAPTER";
  private List<PostModel> postList;
  private PostItemOnclick onclick;


  /**
   * Viewholder item for storing post view bindings
   */
  class PostItemVH extends RecyclerView.ViewHolder {
    // since databinding, the viewholder only needs to contain autogenerated binding object
    final PostItemBinding postBinding;

    public PostItemVH(PostItemBinding postBinding) {
      super(postBinding.getRoot());
      this.postBinding = postBinding;
    }
  }

  public PostItemAdapter(PostItemOnclick onclick) {
    this.onclick = onclick;
  }

  @NonNull
  @Override
  public PostItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // Extra declaration for clarity and readability
    PostItemBinding postBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
        R.layout.post_item, parent, false);

    // attach the onclick object to the binding
    postBinding.setOnclick(this.onclick);
    return new PostItemVH(postBinding);
  }

  @Override
  public void onBindViewHolder(@NonNull PostItemVH holder, int position) {
    // attach the current post to the viewholder's post binding
    holder.postBinding.setPostModel(postList.get(position));
    holder.postBinding.executePendingBindings();
  }

  @Override
  public int getItemCount() {
    return postList == null ? 0 : postList.size();
  }


  public void setPostList(List<PostModel> newPostList) {
    if (postList == null) {
      postList = newPostList;
      notifyItemRangeRemoved(0, newPostList.size());
    } else {
      // used to tell list what has changed
      DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
          return postList.size();
        }

        @Override
        public int getNewListSize() {
          return newPostList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
          return postList.get(oldItemPosition).getId().equals(newPostList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
          return postList.get(oldItemPosition).getId().equals(newPostList.get(newItemPosition).getId());
        }
      });
      // sets the new list as the current one
      postList = newPostList;
      diffResult.dispatchUpdatesTo(this);
    }
  }


  public void resetPostList() {
    postList.clear();
    notifyDataSetChanged();
  }

}
