package com.relic.data.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class PostEntity {
  // api metadata

  // every post is associated with a listing (many posts to one listing)
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public String name;
//  public String getSubreddit_id;
//  public String postListingId;

  // content columns
  public String title;
  public String author;
  public String permalink;
  public String selftext;
  //public String media_embed;
  public int score;
  //public int likes;
  public int ups;
  public int downs;
  @ColumnInfo(name = "commentCount")
  public int num_comments;
  public String created;

  //public String media;
  //public String preview;
  public String thumbnail;
  public String url;

  // post metadata
  public int view_count;
  public boolean visited;

  @Ignore
  //private boolean isEdited;

  public boolean over_18;
  public boolean locked;
  public boolean archived;
  public boolean is_video;
  public boolean is_self;
  public boolean spoiler;
  public String subreddit;

}
