package com.ktvipin.cameraxsample

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

/**
 * Created by Vipin KT on 18/06/20
 */
object AnimationUtils {
    fun startScaleAnimation(
        scaleX: Float,
        scaleY: Float,
        view: View,
        onAnimationStart: (() -> Unit)? = null,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val scaleDownX = ObjectAnimator.ofFloat(
            view, "scaleX", scaleX
        )
        val scaleDownY = ObjectAnimator.ofFloat(
            view, "scaleY", scaleY
        )
        scaleDownX.duration = 500
        scaleDownY.duration = 500
        val scaleDown2 = AnimatorSet()
        scaleDown2.play(scaleDownX).with(scaleDownY)
        scaleDown2.start()
        scaleDown2.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                onAnimationStart?.invoke()
            }

            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
        })
    }

    fun startBlinkAnimation(view: View) {
        AlphaAnimation(0.0f, 1.0f)
            .apply {
                duration = 500
                startOffset = 20
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }.also {
                view.startAnimation(it)
            }
    }

    fun startRotateAnimation(view: View) {
        val start = if (view.rotationY == 0f || view.rotationY == 360f) 0f else 1f
        val end = if (view.rotationY == 0f || view.rotationY == 360f) 1f else 0f
        ValueAnimator
            .ofFloat(start, end)
            .apply {
                duration = 500
                addUpdateListener { pAnimation ->
                    val value = pAnimation.animatedValue as Float
                    view.rotationY = 180 * value
                }
            }
            .start()
    }
}