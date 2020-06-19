package com.ktvipin.cameraxsample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.ImageCapture
import com.ktvipin.cameraxsample.AnimationUtils.startBlinkAnimation
import com.ktvipin.cameraxsample.AnimationUtils.startRotateAnimation
import com.ktvipin.cameraxsample.AnimationUtils.startScaleAnimation
import java.util.concurrent.TimeUnit


/**
 * Created by Vipin KT on 18/06/20
 */
class ControlView : LinearLayout {

    interface Listener {
        fun toggleCamera()
        fun toggleFlash(flashMode: Int)
        fun startVideoCapturing()
        fun stopVideoCapturing()
        fun capturePhoto()
    }

    private var isVideoCapturing: Boolean = false
    private var mFlashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var listener: Listener? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private var startHTime = 0L
    private var timerHandler = Handler()
    private val timerThread = object : Runnable {
        override fun run() {
            tvTimer.text = calculateDuration(SystemClock.uptimeMillis() - startHTime)
            timerHandler.postDelayed(this, 0)
        }
    }

    private val layoutTimer = LinearLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            background = context.getDrawable(R.drawable.bg_rounded_corner_gray)
            gravity = Gravity.CENTER
            setPadding(5.px, 2.px, 5.px, 2.px)
            visibility = View.INVISIBLE
        }
    }.also { addView(it) }

    private val ivRedDot = ImageView(context).apply {
        layoutParams = LayoutParams(6.px, 6.px).apply {
            setImageResource(R.drawable.ic_red_dot_6dp)
            setMargins(leftMargin, topMargin, 5.px, bottomMargin)
            gravity = Gravity.CENTER
        }
    }.also { layoutTimer.addView(it) }

    private val tvTimer = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = "00:00"
            setPaddingRelative(8, 5, 8, 5)
        }
    }.also { layoutTimer.addView(it) }

    private val layoutControls = LinearLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            orientation = HORIZONTAL
            setMargins(leftMargin, 5.px, rightMargin, bottomMargin)
        }
    }.also { addView(it) }

    private val ivFlash = ImageView(context).apply {
        layoutParams = LayoutParams(24.px, 24.px).apply {
            setImageResource(R.drawable.ic_flash_off_white_20dp)
            //setMargins(leftMargin, topMargin, 5.px, bottomMargin)
            gravity = Gravity.CENTER
        }
        setOnClickListener { toggleFlash() }
    }.also { layoutControls.addView(it) }

    private val ivCapture = ImageView(context).apply {
        layoutParams = LayoutParams(70.px, 70.px).apply {
            setImageResource(R.drawable.ic_circle_line_white_24dp)
            setMargins(70.px, 20.px, 70.px, 20.px)
            gravity = Gravity.CENTER
        }
    }.also {
        setupCaptureButtonListener(it)
        layoutControls.addView(it)
    }

    private val ivSwapCam = ImageView(context).apply {
        layoutParams = LayoutParams(24.px, 24.px).apply {
            setImageResource(R.drawable.ic_camera_swap_fill_white_24dp)
            //setMargins(leftMargin, topMargin, 5.px, bottomMargin)
            gravity = Gravity.CENTER
            setOnClickListener {
                startRotateAnimation(it)
                listener?.toggleCamera()
            }
        }
    }.also { layoutControls.addView(it) }

    private val tvInfo = TextView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setMargins(leftMargin, 5.px, rightMargin, bottomMargin)
            text = "Hold for Video, tap for Photo"
        }
    }.also { addView(it) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setPadding(16.px, 16.px, 16.px, 16.px)
        setBackgroundColor(Color.BLUE)
        gravity = Gravity.CENTER
        orientation = VERTICAL
    }

    private fun toggleFlash() {
        when (mFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                mFlashMode = ImageCapture.FLASH_MODE_ON
                ivFlash.setImageResource(R.drawable.ic_flash_on_white_20dp)
            }
            ImageCapture.FLASH_MODE_ON -> {
                mFlashMode = ImageCapture.FLASH_MODE_AUTO
                ivFlash.setImageResource(R.drawable.ic_flash_auto_white_20dp)
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                mFlashMode = ImageCapture.FLASH_MODE_OFF
                ivFlash.setImageResource(R.drawable.ic_flash_off_white_20dp)
            }
        }
        listener?.toggleFlash(mFlashMode)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupCaptureButtonListener(capture: ImageView) {
        var initialTouchX = 0f
        var initialTouchY = 0f
        val mHandler = Handler()
        val mLongPressed = Runnable {
            startVideoCapturing()
        }
        capture.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mHandler.postDelayed(
                        mLongPressed,
                        DELAY_MILLIS
                    )
                    capture.setImageResource(R.drawable.ic_circle_red_white_24dp)
                    startScaleAnimation(
                        SCALE_UP,
                        SCALE_UP, v
                    )
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    stopTimer()
                    mHandler.removeCallbacks(mLongPressed)
                    startScaleAnimation(
                        SCALE_DOWN,
                        SCALE_DOWN, v,
                        onAnimationEnd = {
                            capture.setImageResource(R.drawable.ic_circle_line_white_24dp)
                        }
                    )
                    val xDiff = initialTouchX - event.rawX
                    val yDiff = initialTouchY - event.rawY
                    if ((kotlin.math.abs(xDiff) < 5) && (kotlin.math.abs(yDiff) < 5)) {
                        if (isVideoCapturing) {
                            isVideoCapturing = false
                            listener?.stopVideoCapturing()
                        } else {
                            listener?.capturePhoto()
                        }
                    } else {
                        isVideoCapturing = false
                        listener?.stopVideoCapturing()
                    }
                    v.performClick()
                    return@setOnTouchListener true
                }
                else -> {
                    return@setOnTouchListener false
                }
            }
        }
    }


    private fun startVideoCapturing() {
        ivCapture.performHapticFeedback(ivCapture.id)
        isVideoCapturing = true
        listener?.startVideoCapturing()
        startHTime = SystemClock.uptimeMillis()
        timerHandler.postDelayed(timerThread, 0)
        layoutTimer.visibility = View.VISIBLE
        startBlinkAnimation(ivRedDot)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerThread)
        ivRedDot.clearAnimation()
        layoutTimer.visibility = View.INVISIBLE
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

    companion object {
        private const val TAG = "CameraFragment"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val DELAY_MILLIS = 1000L
        private const val SCALE_UP = 1.5f
        private const val SCALE_DOWN = 1.0f
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val NO_FLASH = -1111
    }
}