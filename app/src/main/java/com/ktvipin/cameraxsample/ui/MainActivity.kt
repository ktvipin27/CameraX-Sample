package com.ktvipin.cameraxsample.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import com.ktvipin.cameraxsample.R


const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

class MainActivity : AppCompatActivity() {
    private val navController by lazy { findNavController(R.id.container) }
    private val startDestination by lazy { navController.graph.startDestination }
    private val currentDestination: Int?
        get() = navController.currentDestination?.id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )
    }

    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (currentDestination == startDestination)
            when (keyCode) {
                in listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN) -> {
                    val intent =
                        Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        else super.onKeyDown(keyCode, event)
    }
}