package com.mytran.myapplication

import android.Manifest.permission.RECEIVE_SMS
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mytran.myapplication.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(RECEIVE_SMS), 9999)
    }
}