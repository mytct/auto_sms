package com.mytran.myapplication.ui.main.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AutoSmsService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

}