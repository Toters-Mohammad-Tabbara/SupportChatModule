package com.toters.twilio_chat_module.extensions

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.*

fun View.updateMarginsRelative(
    @Px start: Int = marginStart,
    @Px top: Int = marginTop,
    @Px end: Int = marginEnd,
    @Px bottom: Int = marginBottom
)  {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
        updateMarginsRelative(
            start, top, end, bottom
        )
    }
}

fun View.rateClickAnimation(scale: Float, colorStart: Int, colorEnd: Int) {
    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd)
    colorAnimation.addUpdateListener { valueAnimator: ValueAnimator ->
        DrawableCompat.setTint(
            background,
            valueAnimator.animatedValue as Int
        )
    }
    colorAnimation.start()
    val bounceList = floatArrayOf(1f, scale, 1f, 1f - scale * 0.2f, 1f, 1f + scale * 0.02f, 1f)
    ObjectAnimator.ofFloat(this, "scaleX", *bounceList).setDuration(400).start()
    ObjectAnimator.ofFloat(this, "scaleY", *bounceList).setDuration(400).start()
}