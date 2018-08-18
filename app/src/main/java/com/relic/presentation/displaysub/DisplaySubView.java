package com.relic.presentation.displaysub;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.relic.R;
import com.relic.data.PostRepository;
import com.relic.data.PostRepositoryImpl;
import com.relic.data.SubRepositoryImpl;
import com.relic.data.models.PostModel;
import com.relic.data.models.SubredditModel;
import com.relic.databinding.DisplaySubBinding;
import com.relic.presentation.DisplayImageFragment;
import com.relic.presentation.adapter.ImageOnClick;
import com.relic.presentation.adapter.PostItemAdapter;
import com.relic.presentation.adapter.PostItemOnclick;
import com.relic.presentation.displaypost.DisplayPostView;
import com.squareup.picasso.Picasso;

import java.util.List;


public class DisplaySubView extends Fragment {
  private final String TAG = "DISPLAYSUB_VIEW";
  private final String SCROLL_POSITION = "POSITION";
  protected DisplaySubContract.ViewModel displaySubVM;

  private SearchView searchView;
  private MenuItem searchMenuItem;
  private Toolbar myToolbar;

  private DisplaySubBinding displaySubBinding;
  private PostItemAdapter postAdapter;
  private SwipeRefreshLayout swipeRefresh;
  private AppBarLayout appBarLayout;
  private String subName;
  private boolean vmAlreadyInitialized;
  private boolean fragmentOpened;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    if (this.getArguments() != null) {
      // parse the SubredditModel from the arguments
      String subredditName = this.getArguments().getString("SubredditName");
      subName = subredditName;

      if (subredditName != null && getActivity()!= null) {
        // get the viewmodel and inject the dependencies into it
        displaySubVM = ViewModelProviders.of(getActivity()).get(DisplaySubVM.class);
        vmAlreadyInitialized = displaySubVM.isInitialized();
        displaySubVM.init(subredditName, new SubRepositoryImpl(this.getContext()), new PostRepositoryImpl(this.getContext()));
      }
    } else {
      Toast.makeText(this.getContext(), "There was an issue loading this sub", Toast.LENGTH_SHORT).show();
    }
  }


  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    // initialize the databinding for the layout
    displaySubBinding = DataBindingUtil.inflate(inflater, R.layout.display_sub, container, false);

    // initialize the post item adapter and attach it to the autogenerated view class
    postAdapter = new PostItemAdapter(new PostItemOnClick(), new OnClickImage());
    displaySubBinding.displayPostsRecyclerview.setAdapter(postAdapter);
    displaySubBinding.setDisplaySubVM(displaySubVM);
    // initialize the reference to the swiperefresh layout
    swipeRefresh = displaySubBinding.getRoot().findViewById(R.id.display_posts_swipeRefreshLayout);

    // attach the listeners responsible checking if the user has scrolled to the bottom of the view
    attachScrollListeners();
    // initialize menu onclicks
    initializeActionbar();
    return displaySubBinding.getRoot();
  }


  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // Subscribe to livedata exposed by the viewmodel
    subscribeToVM();

    // recreate saved the saved instance instance
    if (savedInstanceState != null) {
      Integer position = savedInstanceState.getInt(SCROLL_POSITION);
      Log.d(TAG, "Previous position = " + position);
      // scroll to the previous position before reconfiguration change
      // Temporary fix until a better solution is found for jumping to last view position on
      //displaySubBinding.displayPostsRecyclerview.smoothScrollToPosition(position);
    }
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    //inflater.inflate(R.menu.search_menu, menu);
    inflater.inflate(R.menu.display_sub_menu, menu);
    //menu.findItem(R.id.display_sub_hot).getSubMenu().addSubMenu(R.menu.order_scope_menu);

    searchMenuItem = menu.findItem(R.id.display_sub_searchitem);
    searchView = (SearchView) searchMenuItem.getActionView();
    int padding = (int) getResources().getDimension(R.dimen.search_padding);
    searchView.setPadding(0, 0, padding, padding);

    // Add query listeners to the searchview
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String s) {
        Toast.makeText(getContext(), "Display sub view " + s, Toast.LENGTH_SHORT).show();
        return false;
      }

      @Override
      public boolean onQueryTextChange(String s) {
        Toast.makeText(getContext(), "Display sub view " + s, Toast.LENGTH_SHORT).show();
        return false;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean override = true;
    boolean validOption = false;

    int actionCode = PostRepository.SORT_DEFAULT;
    int scopeCode = PostRepository.SCOPE_NONE;

    switch (item.getItemId()) {
      case R.id.display_sub_best: actionCode = PostRepository.SORT_BEST; validOption = true; break;
      case R.id.display_sub_controversial: actionCode = PostRepository.SORT_CONTROVERSIAL; validOption = true; break;
      case R.id.display_sub_hot: actionCode = PostRepository.SORT_HOT; validOption = true; break;
      case R.id.display_sub_new: actionCode = PostRepository.SORT_NEW; validOption = true; break;
      case R.id.display_sub_rising: actionCode = PostRepository.SORT_RISING; validOption = true; break;
      case R.id.display_sub_top: actionCode = PostRepository.SORT_TOP; validOption = true; break;
      default: override = super.onOptionsItemSelected(item);
    }

    // perform the sort if anything one of the valid sorting options have been selected
    if (validOption) {
      Toast.makeText(getContext(), "Sorting option selected " + actionCode, Toast.LENGTH_SHORT).show();
      // update the view to reflect the refresh action
      appBarLayout.setExpanded(true);
      swipeRefresh.setRefreshing(true);
      // delete current items in the adapter
      postAdapter.clearCurrentPosts();

      // tell vm to coordinate the change
      displaySubVM.changeSortingMethod(actionCode, scopeCode);
    }

    return override;
  }

  /**
   * Observe all the livedata exposed by the viewmodel and attach the appropriate event listeners
   */
  private void subscribeToVM() {
    // observe the livedata list of posts for this subreddit
    displaySubVM.getPosts().observe(this, (@Nullable List<PostModel> postModels) -> {
        Log.d(TAG, "Post models retrieved");
        if (postModels != null) {
          Log.d(TAG, "size of " + postModels.size());
          postAdapter.setPostList(postModels);
          displaySubBinding.executePendingBindings();

          swipeRefresh.setRefreshing(false);
        }
    });

    // observe the subreddit model representing this subreddit
    displaySubVM.getSubModel().observe(this, (@Nullable SubredditModel subredditModel) -> {
        if (subredditModel != null) {
          displaySubBinding.setSubModel(subredditModel);
        }
    });

  }


  /**
   * Attach the event listeners for scrolling within the recyclerview and swiperefreshlayout
   */
  public void attachScrollListeners() {
    // attach listener for checking if the user has scrolled to the bottom of the recyclerview
    displaySubBinding.displayPostsRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);

        // checks if the recyclerview can no longer scroll downwards
        if (!recyclerView.canScrollVertically(1)) {
          // fetch the next post listing
          displaySubVM.retrieveMorePosts(false);
          Log.d(TAG, "Bottom reached, more posts retrieved");
        }
      }
    });

    // Attach listener for refreshing the sub
    swipeRefresh.setOnRefreshListener(() -> {
      // empties current items to show that it's being refreshed
      displaySubBinding.displayPostsRecyclerview.getLayoutManager().scrollToPosition(0);
      postAdapter.clearCurrentPosts();

      // tells vm to clear the posts -> triggers action to retrieve more
      displaySubVM.retrieveMorePosts(true);
      Log.d(TAG, "Top pulled, posts refreshed");
    });
  }


  /**
   * Initializes actionbar menus and on clicks
   */
  private void initializeActionbar() {
    // initialize reference to the toolbar and appbarlayout
    myToolbar = displaySubBinding.getRoot().findViewById(R.id.display_sub_toolbar);
    appBarLayout = displaySubBinding.getRoot().findViewById(R.id.display_sub_appbarlayout);

    TextView title = myToolbar.findViewById(R.id.my_toolbar_title);
    CollapsingToolbarLayout collapsingToolbarLayout = displaySubBinding.getRoot().findViewById(R.id.display_sub_collapsingtoolbarlayout);

    myToolbar.setTitle(subName);
    myToolbar.setSubtitle("Sorting by new");

    AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
    if (parentActivity != null) {
      parentActivity.setSupportActionBar(myToolbar);
    }

    // set onclick to display sub info when the title is clicked
    appBarLayout.findViewById(R.id.display_sub_toolbar).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Toast.makeText(getActivity(), "Title Clicked", Toast.LENGTH_SHORT).show();

        DisplaySubInfoView displaySubInfoView = new DisplaySubInfoView();
        Bundle bundle = new Bundle();
        bundle.putString("name", subName);

        displaySubInfoView.setArguments(bundle);
        displaySubInfoView.show(getActivity().getSupportFragmentManager(), TAG);
      }
    });

    // adds listener for state change for the appbarlayout issue that always opens it when
    // returning to this fragment after popping another off of the stack
    appBarLayout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
      @Override
      public void onViewAttachedToWindow(View view) {
        if (fragmentOpened) {
          displaySubBinding.displaySubBanner.setVisibility(View.GONE);
          appBarLayout.setExpanded(false);
          fragmentOpened = false;
          displaySubBinding.displaySubBanner.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onViewDetachedFromWindow(View view) {
//        displaySubBinding.displaySubBanner.setVisibility(View.GONE);
//        appBarLayout.setExpanded(false);
//        fragmentOpened = false;
//        displaySubBinding.displaySubBanner.setVisibility(View.VISIBLE);
      }
    });

//    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
//      @Override
//      public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
//        boolean collapsed = offset > appBarLayout.getTotalScrollRange();
//        myToolbar.setSubtitle("Sorting by hot");
//      }
//    });
  }


  /**
   * Onclick class for clicking on posts
   */
  private class PostItemOnClick implements PostItemOnclick {
    public void onClick(String postId, String subreddit) {
      // create a new bundle for the post id
      Bundle bundle = new Bundle();
      bundle.putString("full_name", postId);
      bundle.putString("subreddit", subreddit);

      DisplayPostView postFrag = new DisplayPostView();
      postFrag.setArguments(bundle);

      if (getActivity() != null) {
        getActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_content_frame, postFrag).addToBackStack(TAG).commit();
        // set flag to show that a fragment has opened
        fragmentOpened = true;
      }

    }
  }


  /**
   * Onclick class for imageview onclick
   */
  private class OnClickImage implements ImageOnClick {
    @Override
    public void onClick(String url) {
      // Parses the url type and routes it appropriately
      String urlEnding = url.substring(url.length() - 3);
      if (urlEnding.equals("jpg") || urlEnding.equals("png") || urlEnding.equals("gif")) {
        // create a new bundle for to pass the image url along
        Bundle bundle = new Bundle();
        bundle.putString("image_url", url);

        DisplayImageFragment displayImageFragment = new DisplayImageFragment();
        displayImageFragment.setArguments(bundle);

        // replace the current fragment with the new display image frag and add it to the frag stack
        getActivity().getSupportFragmentManager().beginTransaction()
            .add(R.id.main_content_frame, displayImageFragment).addToBackStack(TAG).commit();
      } else {
        // open the url in the browser
        Intent openInBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(openInBrowser);
      }
    }
  }


  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    LinearLayoutManager manager = (LinearLayoutManager) displaySubBinding.displayPostsRecyclerview.getLayoutManager();
    // put the first visible item position into the bundle to allow us to get back to it
    outState.putInt(SCROLL_POSITION, manager.findFirstCompletelyVisibleItemPosition());
    Log.d(TAG, "First position = " + manager.findFirstCompletelyVisibleItemPosition());
  }


  @BindingAdapter({"bind:bannerImage"})
  public static void loadBannerImage (ImageView bannerImageView, String bannerUrl) {
    if (bannerUrl != null && !bannerUrl.isEmpty()) {
      // does not load image if the banner img string is empty
      try {
        Log.d("DISPLAY_SUB_VIEW", "URL = " + bannerUrl);
        Picasso.get().load(bannerUrl).fit().centerCrop().into(bannerImageView);
      }
      catch (Error e) {
        Log.d("DISPLAY_SUB_VIEW", "Issue loading image " + e.toString());
      }
    }
  }

}
