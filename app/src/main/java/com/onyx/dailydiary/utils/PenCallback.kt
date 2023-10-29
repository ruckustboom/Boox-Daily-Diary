package com.onyx.dailydiary.utils

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.rx.RxManager

class PenCallback(
    private val context: Context,
    private val view: BitmapView,
) : RawInputCallback() {
    lateinit var touchHelper: TouchHelper

    override fun onBeginRawDrawing(p0: Boolean, p1: TouchPoint) {}
    override fun onEndRawDrawing(p0: Boolean, p1: TouchPoint?) {}
    override fun onRawDrawingTouchPointMoveReceived(p0: TouchPoint) {}

    override fun onRawDrawingTouchPointListReceived(p0: TouchPointList) {
        view.drawStroke(p0.points)
    }

    override fun onBeginRawErasing(p0: Boolean, p1: TouchPoint) {}
    override fun onEndRawErasing(p0: Boolean, p1: TouchPoint) {}
    override fun onRawErasingTouchPointMoveReceived(p0: TouchPoint) {}
    override fun onRawErasingTouchPointListReceived(p0: TouchPointList) {}

    private val rxManager by lazy {
        RxManager.Builder.initAppContext(context)
        RxManager.Builder.sharedSingleThreadManager()
    }

    override fun onPenUpRefresh(refreshRect: RectF) {
        Log.d(TAG, "onPenUpRefresh")
        val viewRect = RectF(
            refreshRect.left,
            refreshRect.top,
            refreshRect.right,
            refreshRect.bottom,
        )
        val limit = Rect()
        val offset = Point()
        view.getGlobalVisibleRect(limit, offset)
        viewRect.offset(-offset.x.toFloat(), -offset.y.toFloat())
        rxManager.enqueue(
            PartialRefreshRequest(context, view, viewRect, view.bitmap),
            PartialRefreshCallback(),
        )

        // Release raw drawing briefly to allow system events to update the screen
        touchHelper.setRawDrawingEnabled(false)
        Log.d(TAG, "Raw drawing released")
        touchHelper.setRawDrawingEnabled(true)
    }

    companion object {
        private val TAG = PenCallback::class.java.simpleName
    }
}