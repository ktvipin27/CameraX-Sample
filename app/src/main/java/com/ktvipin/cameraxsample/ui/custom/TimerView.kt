package com.ktvipin.cameraxsample.ui.custom

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.utils.AnimationUtils.startBlinkAnimation
import com.ktvipin.cameraxsample.utils.px
import java.util.concurrent.TimeUnit


/**
 * Created by Vipin KT on 18/06/20
 */
class TimerView : LinearLayout {

    private var startTime = 0L
    private var timerHandler = Handler()
    private val timerThread = object : Runnable {
        override fun run() {
            tvTimer.text = calculateDuration(SystemClock.uptimeMillis() - startTime)
            timerHandler.postDelayed(this, 0)
        }
    }

    private val ivRedDot = ImageView(context).apply {
        layoutParams = LayoutParams(6.px, 6.px).apply {
            setImageResource(R.drawable.ic_red_dot_6dp)
            setMargins(leftMargin, topMargin, 5.px, bottomMargin)
            gravity = Gravity.CENTER
        }
    }.also { addView(it) }

    private val tvTimer = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = "00:00"
            setPaddingRelative(8, 5, 8, 5)
        }
    }.also { addView(it) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        background = context.getDrawable(R.drawable.bg_rounded_corner_gray)
        gravity = Gravity.CENTER
        setPadding(5.px, 2.px, 5.px, 2.px)
        visibility = View.INVISIBLE
    }

    fun startTimer() {
        startTime = SystemClock.uptimeMillis()
        timerHandler.postDelayed(timerThread, 0)
        visibility = View.VISIBLE
        ivRedDot.startBlinkAnimation()
    }

    fun stopTimer() {
        timerHandler.removeCallbacks(timerThread)
        ivRedDot.clearAnimation()
        visibility = View.INVISIBLE
        tvTimer.text = "00:00"
    }

    private fun calculateDuration(timeInMilliseconds: Long): String {
        return String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds)).plus(
            ":" + String.format(
                "%02d",
                TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds)
                )
            )
        )
    }
}