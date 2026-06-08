package com.ornitrack.raw

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.ornitrack.raw.data.database.AppDatabase

class OrniTrackApp : Application(), Configuration.Provider {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        lateinit var instance: OrniTrackApp
            private set
    }
}