package com.relic.deserializer.response

/**
 * Just some example responses for testing. This is by no means exhaustive
 * and will merely serve as a sanity check
 */
const val SELF_TEXT = """
&gt; Let\u2019s say you\u2019re 
"""

const val EXPECTED_SELF_TEXT = """
> Let’s say you’re 
"""

const val POST = """
{
    "kind": "t3",
    "data": {
        "approved_at_utc": null,
        "subreddit": "worldbuilding",
        "selftext": "$SELF_TEXT",
        "user_reports": [],
        "saved": false,
        "mod_reason_title": null,
        "gilded": 0,
        "clicked": false,
        "title": "Worldbuilding Community Programs for July!",
        "link_flair_richtext": [],
        "subreddit_name_prefixed": "r/worldbuilding",
        "hidden": false,
        "pwls": 6,
        "link_flair_css_class": "resource",
        "downs": 0,
        "thumbnail_height": null,
        "parent_whitelist_status": "all_ads",
        "hide_score": false,
        "name": "t3_c5iat0",
        "quarantine": false,
        "link_flair_text_color": "light",
        "upvote_ratio": 0.97,
        "author_flair_background_color": null,
        "subreddit_type": "public",
        "ups": 30,
        "total_awards_received": 0,
        "media_embed": {},
        "thumbnail_width": null,
        "author_flair_template_id": "5762b2ca-08a2-11e5-b16d-0e6f891f9a0b",
        "is_original_content": false,
        "author_fullname": "t2_j6uko",
        "secure_media": null,
        "is_reddit_media_domain": false,
        "is_meta": false,
        "category": null,
        "num_comments": 0,
        "secure_media_embed": {},
        "link_flair_text": "Resource",
        "can_mod_post": false,
        "score": 30,
        "approved_by": null,
        "thumbnail": "self",
        "edited": false,
        "author_flair_css_class": null,
        "author_flair_richtext": [],
        "gildings": {},
        "post_hint": "self",
        "content_categories": null,
        "is_self": true,
        "mod_note": null,
        "created": 1561538002,
        "link_flair_type": "text",
        "wls": 6,
        "banned_by": null,
        "author_flair_type": "text",
        "domain": "self.worldbuilding",
        "selftext_html": "test",
        "likes": null,
        "suggested_sort": null,
        "banned_at_utc": null,
        "view_count": null,
        "archived": false,
        "no_follow": false,
        "is_crosspostable": true,
        "pinned": false,
        "over_18": false,
        "preview": {
            "images": [
                {
                    "source": {
                        "url": "https://external-preview.redd.it/89UDFQfUFFAHLPVDoSqae-mPnX_Bb-U4IQyQNKzEm10.jpg?auto=webp&amp;s=cc2cb22a03bd037556221346772d3812ff01f8e5",
                        "width": 256,
                        "height": 256
                    },
                    "resolutions": [
                        {
                            "url": "https://external-preview.redd.it/89UDFQfUFFAHLPVDoSqae-mPnX_Bb-U4IQyQNKzEm10.jpg?width=108&amp;crop=smart&amp;auto=webp&amp;s=4204914eb57e035934db54dfd52431dba9e8553a",
                            "width": 108,
                            "height": 108
                        },
                        {
                            "url": "https://external-preview.redd.it/89UDFQfUFFAHLPVDoSqae-mPnX_Bb-U4IQyQNKzEm10.jpg?width=216&amp;crop=smart&amp;auto=webp&amp;s=c02c5bb2ea38a653f897a6842e22870dd2eaaf4c",
                            "width": 216,
                            "height": 216
                        }
                    ],
                    "variants": {},
                    "id": "9BoAwy1DmMQfkdMHJb4NoyvSWH0EVIIiCZ9TKAXsZ_M"
                }
            ],
            "enabled": false
        },
        "all_awardings": [],
        "media_only": false,
        "link_flair_template_id": "e56cc21c-8a3e-11e6-8305-0e7a198676f1",
        "can_gild": true,
        "spoiler": false,
        "locked": false,
        "author_flair_text": "SR. MOD | Horror Shop, a Gothic Punk Urban Fantasy",
        "visited": false,
        "num_reports": null,
        "distinguished": null,
        "subreddit_id": "t5_2rd6n",
        "mod_reason_by": null,
        "removal_reason": null,
        "link_flair_background_color": "#373c3f",
        "id": "c5iat0",
        "is_robot_indexable": true,
        "report_reasons": null,
        "author": "the_vizir",
        "num_crossposts": 0,
        "media": null,
        "send_replies": true,
        "contest_mode": false,
        "author_patreon_flair": false,
        "author_flair_text_color": "dark",
        "permalink": "/r/worldbuilding/comments/c5iat0/worldbuilding_community_programs_for_july/",
        "whitelist_status": "all_ads",
        "stickied": true,
        "url": "https://www.reddit.com/r/worldbuilding/comments/c5iat0/worldbuilding_community_programs_for_july/",
        "subreddit_subscribers": 408899,
        "created_utc": 1561509202,
        "mod_reports": [],
        "is_video": false
    }
}  
"""

const val POST_RESPONSE = """
[
    {
        "kind": "Listing",
        "data": {
            "modhash": null,
            "dist": 1,
            "children": [$POST],
            "after": null,
            "before": null
        }
    },
    {
        "kind": "Listing",
        "data": {
            "modhash": null,
            "dist": null,
            "children": [],
            "after": null,
            "before": null
        }
    }
]
"""

const val POSTS_RESPONSE = """
{
    "kind": "Listing",
    "data": {
        "modhash": null,
        "dist": 27,
        "children": [$POST, $POST],
        "after": "t3_c7566d",
        "before": null
    }
}"""