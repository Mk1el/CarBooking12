package com.example.carbooking

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CarBookingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:667827563446:android:0e97dcd2513b1a692cef17")
                .setApiKey("AIzaSyCT4rH9wKMb-oe6sKcG1ZNXSYfzzq1dD6c")
                .setProjectId("carboooking-93a28")
                .build()

            FirebaseApp.initializeApp(this, options)
        }
    }
}