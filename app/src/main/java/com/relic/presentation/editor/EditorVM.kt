package com.relic.presentation.editor

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.relic.data.CommentRepository
import com.relic.data.PostRepository

class EditorVM : EditorContract.VM, ViewModel() {
    private var isInitialized : Boolean = false

    private lateinit var postRepo : PostRepository

    private val parentModelText = MediatorLiveData<String>()

    override fun isInitialized(): Boolean = isInitialized

    override fun init(
            subName: String,
            fullName: String,
            parentType: Int,
            postRepository: PostRepository,
            commentRepository: CommentRepository) {

        postRepo = postRepository

        Log.d("editorvm", "fullname = " + fullName)
        if (parentType == EditorContract.VM.POST_PARENT) {
            // retrieve the post from the post repo
            parentModelText.addSource(postRepo.getPost(fullName)) {
                Log.d("TEST", "fullname = " + fullName + ": " + it?.selftext)
                parentModelText.value = it?.selftext
            }
        }
        else if (parentType == EditorContract.VM.COMMENT_PARENT) {
            // TODO add method for retrieving comment model
            //commentRepo.get
        }

        isInitialized = true
    }

    override fun getParentText(): LiveData<String> {
        return parentModelText
    }




}