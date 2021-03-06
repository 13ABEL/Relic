package com.relic.presentation.displayuser

import android.os.Parcelable
import com.relic.domain.models.ListingItem
import kotlinx.android.parcel.Parcelize

interface DisplayUserContract {
    interface View
    interface ViewModel

    interface ListingItemAdapterDelegate {
        fun visitListing(listingItem : ListingItem)
        fun voteOnListing(listingItem : ListingItem, newVote : Int)
        fun saveListing(listingItem : ListingItem)
        fun onThumbnailClicked(listingItem : ListingItem)
        fun onUserClicked(listingItem : ListingItem)
    }
}

sealed class UserTab(
    val tabName : String
) : Parcelable {
    @Parcelize object Submitted: UserTab("Submitted")
    @Parcelize object Comments: UserTab("Comments")
    @Parcelize object Saved: UserTab("Saved")
    @Parcelize object Upvoted: UserTab("Upvoted")
    @Parcelize object Downvoted: UserTab("Downvoted")
    @Parcelize object Gilded: UserTab("Gilded")
    @Parcelize object Hidden: UserTab("Hidden")
}

val selfTabTypes = listOf(UserTab.Submitted, UserTab.Comments, UserTab.Saved, UserTab.Upvoted, UserTab.Downvoted, UserTab.Gilded, UserTab.Hidden)

val tabTypes = listOf(UserTab.Submitted, UserTab.Comments)

sealed class ErrorData {
    data class NoMorePosts(val tab : UserTab) : ErrorData()
}