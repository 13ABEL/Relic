package com.relic

import android.app.Activity
import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.gfycat.core.GfyCoreInitializationBuilder
import com.gfycat.core.GfyCoreInitializer
import com.gfycat.core.GfycatApplicationInfo
import com.relic.dagger.AppInjector
import com.relic.scheduler.RelicWorkerFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import timber.log.Timber
import javax.inject.Inject

class RelicApp : Application(), HasActivityInjector{

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var relicWorkerFactory : RelicWorkerFactory

    override fun onCreate() {
        AppInjector.init(this)
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // opted not to provide GfyCore instance through dagger di since it's already a singleton
        GfyCoreInitializer.initialize(
            GfyCoreInitializationBuilder(this, GfycatApplicationInfo(
                getString(R.string.gfycat_client_id),
                getString(R.string.gfycat_client_secret)
            ))
        )

        // register our custom worker factory
        val config = Configuration.Builder()
          .setWorkerFactory(relicWorkerFactory)
          .build()
        WorkManager.initialize(this, config)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingAndroidInjector
    }

    inner class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // TODO add firebase crashlytics
        }
    }
}