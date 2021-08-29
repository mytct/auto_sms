package com.mytran.myapplication.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import com.mytran.myapplication.ui.main.MainViewModel
import com.mytran.myapplication.ui.main.data.UserPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

private val USER_PREFERENCES_NAME = "sms_auto_preferences"

private val Context.dataStore by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

val viewModelModule = module {
    viewModel { MainViewModel(androidContext().dataStore,
        get()) }
}