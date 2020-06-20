package com.ktvipin.cameraxsample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.ui.main.CameraFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CameraFragment.newInstance())
                .commitNow()
        }
    }
}