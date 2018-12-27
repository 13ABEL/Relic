package com.relic

import android.content.Context
import android.os.PersistableBundle
import android.support.v4.view.MotionEventCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout

import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

import com.relic.data.Authenticator
import com.relic.network.VolleyQueue
import com.relic.presentation.callbacks.AuthenticationCallback
import com.relic.presentation.displaysubs.DisplaySubsView
import com.relic.presentation.home.HomeFragment

import javax.inject.Inject

class MainActivity : AppCompatActivity(), AuthenticationCallback {
    internal val TAG = "MAIN_ACTIVITY"

    private val titleTW: TextView? = null
    private val subtitleTW: TextView? = null

    @Inject
    internal lateinit var auth: Authenticator

    private lateinit var navigationView: NavigationView
    private lateinit var navDrawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as RelicApp).appComponent.inject(this)

        // initialize the request queue and authenticator instance
        VolleyQueue.get(applicationContext)
        auth.refreshToken { this.initializeDefaultView() }

        if (!auth.isAuthenticated) {
            // create the login fragment for the user if not authenticated
            val loginFragment = LoginFragment()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content_frame, loginFragment).commit()
        }

        //    findViewById(R.id.my_toolbar).setOnClickListener(new View.OnClickListener() {
        //      @Override
        //      public void onClick(View view) {
        //        Toast.makeText(getApplicationContext(), "NAVBAR", Toast.LENGTH_SHORT).show();
        //      }
        //    });

        navigationView = findViewById(R.id.navigationView)
        navDrawer = findViewById(R.id.navigationDrawer)
        navigationView.setNavigationItemSelectedListener { handleNavMenuOnclick(it) }
    }

    override fun onAuthenticated() {
        // sends user to default view of subreddits
        initializeDefaultView()
    }

    private fun initializeDefaultView() {
        // get the number of additional (non default) fragments in the stack
        val fragCount = supportFragmentManager.backStackEntryCount
        Log.d(TAG, "Number of fragments $fragCount")

        // add the default view only if there are no additional fragments on the stack
        if (fragCount < 1) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content_frame, HomeFragment())
                    .commit()
        }
    }

    // region navigation view handlers

    private fun handleNavMenuOnclick(item : MenuItem) : Boolean {
        when (item.itemId) {
            R.id.preferences -> navDrawer.closeDrawers()
        }

        return true
    }

    // endregion navigation view handlers

}

