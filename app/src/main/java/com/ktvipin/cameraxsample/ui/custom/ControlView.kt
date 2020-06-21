package com.ktvipin.cameraxsample.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.utils.AnimationUtils.startRotateAnimation
import com.ktvipin.cameraxsample.utils.AnimationUtils.startScaleAnimation
import com.ktvipin.cameraxsample.utils.Config.LONG_PRESS_DELAY_MILLIS
import com.ktvipin.cameraxsample.utils.Config.SCALE_DOWN
import com.ktvipin.cameraxsample.utils.Config.SCALE_UP
import com.ktvipin.cameraxsample.utils.px


/**
 * Created by Vipin KT on 18/06/20
 */
class ControlView : LinearLayout {

    interface Listener {
        fun toggleCamera()
        fun toggleFlash(flashMode: FlashMode)
        fun capturePhoto()
        fun startVideoCapturing()
        fun stopVideoCapturing()
    }

    enum class FlashMode {
        FLASH_MODE_AUTO,
        FLASH_MODE_ON,
        FLASH_MODE_OFF
    }

    private var isVideoCapturing: Boolean = false
    private var flashMode: FlashMode =
        FlashMode.FLASH_MODE_OFF
    private var listener: Listener? = null

    private val timerView = TimerView(context)
        .apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            visibility = View.INVISIBLE
        }.also { addView(it) }

    private val layoutControls = LinearLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            orientation = HORIZONTAL
            setMargins(leftMargin, 5.px, rightMargin, bottomMargin)
        }
    }.also { addView(it) }

    private val ivFlash = ImageView(context).apply {
        layoutParams = LayoutParams(48.px, 48.px).apply {
            setImageResource(R.drawable.ic_flash_off_white_20dp)
            gravity = Gravity.CENTER
        }
        setOnClickListener { toggleFlash() }
        setPadding(12.px, 12.px, 12.px, 12.px)
        visibility = View.INVISIBLE
    }.also { layoutControls.addView(it) }

    private val ivCapture = ImageView(context).apply {
        layoutParams = LayoutParams(70.px, 70.px).apply {
            setImageResource(R.drawable.ic_circle_line_white_24dp)
            setMargins(70.px, 20.px, 70.px, 20.px)
            gravity = Gravity.CENTER
        }
    }.also {
        isHapticFeedbackEnabled = true
        setupCaptureButtonListener(it)
        layoutControls.addView(it)
    }

    private val ivSwitchCam = ImageView(context).apply {
        layoutParams = LayoutParams(48.px, 48.px).apply {
            setImageResource(R.drawable.ic_baseline_flip_camera_24dp)
            gravity = Gravity.CENTER
            setOnClickListener {
                it.startRotateAnimation()
                listener?.toggleCamera()
            }
            setPadding(12.px, 12.px, 12.px, 12.px)
            visibility = View.INVISIBLE
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
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.CENTER
        orientation = VERTICAL
    }

    private fun toggleFlash() {
        when (flashMode) {
            FlashMode.FLASH_MODE_AUTO -> {
                flashMode =
                    FlashMode.FLASH_MODE_OFF
                ivFlash.setImageResource(R.drawable.ic_flash_off_white_20dp)
            }
            FlashMode.FLASH_MODE_ON -> {
                flashMode =
                    FlashMode.FLASH_MODE_AUTO
                ivFlash.setImageResource(R.drawable.ic_flash_auto_white_20dp)
            }
            FlashMode.FLASH_MODE_OFF -> {
                flashMode =
                    FlashMode.FLASH_MODE_ON
                ivFlash.setImageResource(R.drawable.ic_flash_on_white_20dp)
            }
        }
        listener?.toggleFlash(flashMode)
    }

    fun getFlashMode() = flashMode

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCaptureButtonListener(capture: ImageView) {
        var initialTouchX = 0f
        var initialTouchY = 0f
        val mHandler = Handler()
        val mLongPressed = Runnable {
            onLongPressCaptureButton()
        }
        capture.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mHandler.postDelayed(
                        mLongPressed,
                        LONG_PRESS_DELAY_MILLIS
                    )
                    capture.setImageResource(R.drawable.ic_circle_red_white_24dp)
                    v.startScaleAnimation(
                        SCALE_UP,
                        SCALE_UP
                    )
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    timerView.stopTimer()
                    mHandler.removeCallbacks(mLongPressed)
                    v.startScaleAnimation(SCALE_DOWN, SCALE_DOWN) {
                        capture.setImageResource(R.drawable.ic_circle_line_white_24dp)
                    }
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

    private fun onLongPressCaptureButton() {
        ivCapture.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        isVideoCapturing = true
        listener?.startVideoCapturing()
        timerView.startTimer()
    }

    fun setFlashViewVisibility(visibility: Boolean) {
        ivFlash.visibility = if (visibility) View.VISIBLE else View.INVISIBLE
    }

    fun setCameraSwitchVisibility(visibility: Boolean) {
        ivSwitchCam.visibility = if (visibility) View.VISIBLE else View.INVISIBLE
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }
}