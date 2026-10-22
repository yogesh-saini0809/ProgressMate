package com.progressmate.app

import android.app.Application
import com.progressmate.app.data.AppDatabase

class ProgressMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
    }
}
