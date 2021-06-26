package com.example.oilchecker.util

import android.content.Context

/*
val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
*/

object DisplayUtil {

    fun dp2px(context:Context,dp:Int):Int=(dp * context.resources.displayMetrics.density).toInt()

    fun px2dp(context:Context,px:Int):Int =(px / context.resources.displayMetrics.density).toInt()
}