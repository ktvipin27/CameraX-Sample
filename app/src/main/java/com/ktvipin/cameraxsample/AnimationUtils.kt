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
    fun View.startScaleAnimation(
        scaleX: Float,
        scaleY: Float,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val scaleDownX = ObjectAnimator.ofFloat(
            this, "scaleX", scaleX
        )
        val scaleDownY = ObjectAnimator.ofFloat(
            this, "scaleY", scaleY
        )
        scaleDownX.duration = 500
        scaleDownY.duration = 500
        val scaleDown2 = AnimatorSet()
        scaleDown2.play(scaleDownX).with(scaleDownY)
        scaleDown2.start()
        scaleDown2.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd?.invoke()
            }
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
        })
    }

    fun View.startBlinkAnimation() {
        AlphaAnimation(0.0f, 1.0f)
            .apply {
                duration = 500
                startOffset = 20
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }.also {
                startAnimation(it)
            }
    }

    fun View.startRotateAnimation() {
        ValueAnimator
            .ofFloat(0f, 1f)
            .apply {
                duration = 500
                addUpdateListener { pAnimation ->
                    val value = pAnimation.animatedValue as Float
                    rotationY = 180 * value
                }
            }
            .start()
    }
}