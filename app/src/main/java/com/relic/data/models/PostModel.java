package com.relic.data.models;

import android.arch.persistence.room.Ignore;

import com.relic.domain.Post;


public class PostModel implements Post {
  private String id;
  private String selfText;
  private String created;
  private int score;
  public String title;
  private int commentCount;

  public PostModel() {}


  //  public String subName;
//  public String stringDate;
  public int getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(int commentCount) {
    this.commentCount = commentCount;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSelfText() {
    return selfText;
  }

  public void setSelfText(String selfText) {
    this.selfText = selfText;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }


  // Only constructor should be used by room
//  public PostModel(String id, String author, int commentCount, int karma, String title,
//                   String subId, String stringDate) {
//  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }
}
