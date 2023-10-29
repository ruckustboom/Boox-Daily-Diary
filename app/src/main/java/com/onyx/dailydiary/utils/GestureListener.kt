package com.onyx.dailydiary.utils

import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import kotlin.math.abs

open class GestureListener : SimpleOnGestureListener() {
    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onFling(
        event1: MotionEvent?,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        if (event1 == null || !event2.isType(MotionEvent.TOOL_TYPE_FINGER)) return false
        val diffY = event2.y - event1.y
        val diffX = event2.x - event1.x
        Log.d(TAG, "onFling: $diffY $diffX $velocityX $velocityY")
        if (abs(diffX) > abs(diffY)) {
            if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    onSwipeRight()
                } else {
                    onSwipeLeft()
                }
            }
        } else {
            if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeDown()
                } else {
                    onSwipeUp()
                }
            }
        }
        return true
    }

    open fun onSwipeLeft() {
        Log.d(TAG, "onSwipeLeft")
    }

    open fun onSwipeRight() {
        Log.d(TAG, "onSwipeRight")
    }

    open fun onSwipeDown() {
        Log.d(TAG, "onSwipeDown")
    }

    open fun onSwipeUp() {
        Log.d(TAG, "onSwipeUp")
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        Log.d(TAG, "onDoubleTap: $event")
        return true
    }

    companion object {
        private const val SWIPE_VELOCITY_THRESHOLD = 1000
        private const val SWIPE_THRESHOLD = 400
        private val TAG = GestureListener::class.java.simpleName

        fun MotionEvent.isType(toolType: Int): Boolean = MotionEvent.PointerProperties()
            .also { getPointerProperties(0, it) }
            .toolType == toolType
    }
}
