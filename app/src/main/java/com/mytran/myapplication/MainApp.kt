package com.mytran.myapplication

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.mytran.myapplication.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApp: MultiDexApplication() {
    companion object {
        lateinit var instance: MainApp
            private set
    }
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApp)
            modules(listOf(
                //databaseModule,
                viewModelModule,
                apiModule,
                repositoryModule,
                retrofitModule
            ))

        }
        instance = this
    }
}