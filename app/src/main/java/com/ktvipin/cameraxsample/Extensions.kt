package com.ktvipin.cameraxsample

import android.content.res.Resources

/**
 * Created by Vipin KT on 18/06/20
 */
val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()