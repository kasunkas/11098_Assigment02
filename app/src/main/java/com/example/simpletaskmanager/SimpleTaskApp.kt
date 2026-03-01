package com.example.simpletaskmanager

import android.app.Application
import com.example.simpletaskmanager.data.AppDatabase
import com.example.simpletaskmanager.data.UserPreferencesRepository

class SimpleTaskApp : Application() {
    companion object {
        lateinit var database: AppDatabase
        lateinit var userPreferences: UserPreferencesRepository
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        userPreferences = UserPreferencesRepository(this)
    }
}
