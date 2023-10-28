package com.onyx.dailydiary.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import com.onyx.android.sdk.data.note.TouchPoint

class BitmapView : SurfaceView {
    var bitmap: Bitmap? = null
        private set

    private val mStrokeWidth = 4

    private val penPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = mStrokeWidth.toFloat()
    }
    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.WHITE
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.SQUARE
        strokeWidth = (10 * mStrokeWidth).toFloat()
    }
    var isErasing = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        Log.d(TAG, "initView")
        setBackgroundColor(Color.WHITE)
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        Log.d(TAG, "onWindowVisibilityChanged $visibility")
        redrawSurface()
    }

    public override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "onDraw")

        super.onDraw(canvas)
        if (bitmap == null) resetBitmap()
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    public override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?,
    ) {
        Log.d(TAG, "onFocusChanged")
        redrawSurface()
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    fun resetBitmap() {
        Log.d(TAG, "resetBitmap")
        try {
            bitmap = null
            if (!holder.surface.isValid) {
                return
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap!!)
            canvas.drawColor(Color.WHITE)
        } catch (e: Exception) {
            Log.d("resetBitmap Error: ", e.message, e)
        }
    }

    fun redrawSurface() {
        Log.d(TAG, "redrawSurface")
        if (!holder.surface.isValid) return
        val lockCanvas = holder.lockCanvas()
        draw(lockCanvas)
        holder.unlockCanvasAndPost(lockCanvas)
    }

    fun partialRedraw(renderRect: Rect?) {
        if (!holder.surface.isValid) {
            return
        }
        val canvas = holder.lockCanvas(renderRect) ?: return
        try {
            canvas.clipRect(renderRect!!)
            val rect = Rect(0, 0, bitmap!!.width, bitmap!!.height)
            canvas.drawBitmap(bitmap!!, rect, rect, null)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun drawStroke(points: List<TouchPoint>, isAlt: Boolean = false) {
        val canvas = Canvas(bitmap!!)
        val limit = Rect()
        val offset = Point()
        getGlobalVisibleRect(limit, offset)
        val path = Path()
        val prePoint = PointF(points[0].x, points[0].y)
        path.moveTo(prePoint.x - offset.x, prePoint.y - offset.y)
        for (point in points) {
            path.quadTo(
                prePoint.x - offset.x,
                prePoint.y - offset.y,
                point.x - offset.x,
                point.y - offset.y
            )
            prePoint.x = point.x
            prePoint.y = point.y
        }
        canvas.drawPath(path, if (isErasing xor isAlt) eraserPaint else penPaint)
    }

    companion object {
        private val TAG = BitmapView::class.java.simpleName
    }
}
