package com.relic.preference

import android.app.Application
import android.preference.PreferenceManager
import com.relic.preference.ViewPreferenceKeys.KEY_THEME
import com.relic.preference.ViewPreferenceKeys.POST_CARD_STYLE
import javax.inject.Inject

/*
Class for standardizing access to shared preferences
 */
class ViewPreferencesManager @Inject constructor(
    app : Application
) : AppViewPreferences, SubViewPreferences, PostViewPreferences, FullPostViewPreferences {
    private val sp = PreferenceManager.getDefaultSharedPreferences(app)

    // region PostViewPreferences
    override fun setPostCardStyle(cardStyle : Int) = sp.edit().putInt(POST_CARD_STYLE, cardStyle).apply()
    override fun getPostCardStyle() : Int = sp.getInt(POST_CARD_STYLE, 0)
    // endregion PostViewPreferences

    override fun setAppTheme(themeId: Int) = sp.edit().putInt(KEY_THEME, themeId).apply()
    override fun getAppTheme(): Int = sp.getInt(KEY_THEME, 0)


}