package com.onyx.dailydiary.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import com.onyx.android.sdk.rx.RxCallback
import com.onyx.android.sdk.rx.RxRequest
import com.onyx.android.sdk.utils.RectUtils

class PartialRefreshRequest(
    context: Context,
    private val surfaceView: BitmapView,
    private val refreshRect: RectF,
    private val bitmap: Bitmap?,
) : RxRequest() {
    init {
        setContext(context)
    }

    override fun execute() {
        renderToScreen()
    }

    private fun renderToScreen() {
        if (!surfaceView.holder.surface.isValid) {
            return
        }
        val renderRect = RectUtils.toRect(refreshRect)
        val canvas = surfaceView.holder.lockCanvas(renderRect) ?: return
        try {
            canvas.clipRect(renderRect)
            drawRendererContent(bitmap, canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawRendererContent(bitmap: Bitmap?, canvas: Canvas) {
        bitmap ?: return
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, rect, rect, null)
    }
}

class PartialRefreshCallback : RxCallback<PartialRefreshRequest>() {
    override fun onNext(p0: PartialRefreshRequest) {}
}
