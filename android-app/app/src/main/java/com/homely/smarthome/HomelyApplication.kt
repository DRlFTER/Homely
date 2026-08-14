package com.homely.smarthome

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class HomelyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebaseIfNeeded()
    }

    private fun initializeFirebaseIfNeeded() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
            .build()

        FirebaseApp.initializeApp(this, options)
    }
}
