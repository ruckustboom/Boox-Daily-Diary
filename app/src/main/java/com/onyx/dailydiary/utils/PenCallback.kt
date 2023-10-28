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
import com.onyx.android.sdk.rx.RxCallback
import com.onyx.android.sdk.rx.RxManager
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class PenCallback(
    private val context: Context,
    private val view: BitmapView,
) : RawInputCallback() {
    lateinit var touchHelper: TouchHelper

    private var lastDraw = 0L
    var isRawDrawing = false
        private set
    private var isRedrawRunning = false

    private val points = mutableListOf<TouchPoint>()
    private var minTouchX = 0
    private var minTouchY = 0
    private var maxTouchX = 0
    private var maxTouchY = 0

    override fun onBeginRawDrawing(p0: Boolean, p1: TouchPoint) {
        Log.d(TAG, "onBeginRawDrawing")
        minTouchX = p1.x.toInt()
        maxTouchX = p1.x.toInt()
        minTouchY = p1.y.toInt()
        maxTouchY = p1.y.toInt()
        points.clear()
        isRawDrawing = true
    }

    override fun onEndRawDrawing(p0: Boolean, p1: TouchPoint?) {
        Log.d(TAG, "onEndRawDrawing")

        isRawDrawing = false

        // wait until there's been no writing input then
        // restart the touch helper - this is needed to
        // keep the navigation ball working

        // wait until there's been no writing input then
        // restart the touch helper - this is needed to
        // keep the navigation ball working
        lastDraw = System.currentTimeMillis()
        if (!isRedrawRunning) {
            isRedrawRunning = true
            thread {
                var currentTime = System.currentTimeMillis()
                while (currentTime < lastDraw + REFRESH_INTERVAL) {
                    currentTime = System.currentTimeMillis()
                }
                touchHelper.setRawDrawingEnabled(false)
                touchHelper.setRawDrawingEnabled(true)
                isRedrawRunning = false
            }
        }
    }

    override fun onRawDrawingTouchPointMoveReceived(p0: TouchPoint) {
        lastDraw = System.currentTimeMillis()
        minTouchX = min(minTouchX, p0.x.toInt())
        minTouchY = min(minTouchY, p0.y.toInt())
        maxTouchX = max(maxTouchX, p0.x.toInt())
        maxTouchY = max(maxTouchY, p0.y.toInt())
    }

    override fun onRawDrawingTouchPointListReceived(p0: TouchPointList) {
        view.drawStroke(p0.points)
    }

    override fun onBeginRawErasing(p0: Boolean, p1: TouchPoint) {
        Log.d(TAG, "onBeginRawErasing")

        points.clear()
        minTouchX = p1.x.toInt()
        maxTouchX = p1.x.toInt()
        minTouchY = p1.y.toInt()
        maxTouchY = p1.y.toInt()
        isRawDrawing = true
//        for (view in views) {
//            view.redrawSurface()
//        }
    }

    override fun onEndRawErasing(p0: Boolean, p1: TouchPoint) {
        Log.d(TAG, "onEndRawErasing")

        points.add(p1)
        val pointList = points.toList()
        points.clear()
        val touchPointList = TouchPointList()
        for (point in pointList) {
            touchPointList.add(point)
        }
        view.drawStroke(pointList, isAlt = true)
        val eraseRect = Rect(minTouchX, minTouchY, maxTouchX, maxTouchY)
        val limit = Rect()
        val offset = Point()
        view.getGlobalVisibleRect(limit, offset)
        eraseRect.offset(-offset.x, -offset.y)
        view.partialRedraw(eraseRect)
        isRawDrawing = false
    }

    override fun onRawErasingTouchPointMoveReceived(p0: TouchPoint) {
        points.add(p0)
        minTouchX = min(minTouchX, p0.x.toInt())
        minTouchY = min(minTouchY, p0.y.toInt())
        maxTouchX = max(maxTouchX, p0.x.toInt())
        maxTouchY = max(maxTouchY, p0.y.toInt())

        if (points.size >= 50) {
            val pointList = points.toList()
            points.clear()
            val touchPointList = TouchPointList()
            for (point in pointList) {
                touchPointList.add(point)
            }
            view.drawStroke(pointList, isAlt = true)
            val eraseRect = Rect(minTouchX, minTouchY, maxTouchX, maxTouchY)
            val limit = Rect()
            val offset = Point()
            view.getGlobalVisibleRect(limit, offset)
            eraseRect.offset(-offset.x, -offset.y)
            view.partialRedraw(eraseRect)
        }
    }

    override fun onRawErasingTouchPointListReceived(p0: TouchPointList) {
        Log.d(TAG, "onRawErasingTouchPointListReceived")

        view.drawStroke(p0.points, isAlt = true)
        val eraseRect = Rect(minTouchX, minTouchY, maxTouchX, maxTouchY)
        val limit = Rect()
        val offset = Point()
        view.getGlobalVisibleRect(limit, offset)
        eraseRect.offset(-offset.x, -offset.y)
        view.partialRedraw(eraseRect)
    }

    private val rxManager by lazy {
        RxManager.Builder.initAppContext(context)
        RxManager.Builder.sharedSingleThreadManager()
    }

    override fun onPenUpRefresh(refreshRect: RectF) {
        Log.d(TAG, "onPenUpRefresh $isRawDrawing")
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
            object : RxCallback<PartialRefreshRequest>() {
                override fun onNext(partialRefreshRequest: PartialRefreshRequest) {}
            },
        )
    }

    companion object {
        private val TAG = PenCallback::class.java.simpleName
        private const val REFRESH_INTERVAL = 1000L
    }
}