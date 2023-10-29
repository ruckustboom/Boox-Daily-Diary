package com.onyx.dailydiary.writer

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.dailydiary.R
import com.onyx.dailydiary.databinding.ActivityWriterBinding
import com.onyx.dailydiary.utils.GestureListener
import com.onyx.dailydiary.utils.PenCallback
import com.onyx.dailydiary.utils.PenType

class WriterActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityWriterBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var touchHelper: TouchHelper
    private lateinit var penCallback: PenCallback

    private val limitRectList = mutableListOf<Rect>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityWriterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        penCallback = PenCallback(this, binding.writerview)
        touchHelper = TouchHelper.create(window.decorView.rootView, penCallback).apply {
            debugLog(false)
            setRawInputReaderEnable(true)
        }
        penCallback.touchHelper = touchHelper

        // setup the gestures
        gestureDetector = GestureDetectorCompat(this, object : GestureListener() {
            override fun onSwipeDown() {
                confirmClearPage()
            }

            override fun onSwipeLeft() {
                Log.d(TAG, "Swipe Left")
            }

            override fun onSwipeRight() {
                Log.d(TAG, "Swipe Right")
            }

            override fun onSwipeUp() {
                Log.d(TAG, "Swipe Up")
            }
        })

        // initialise surface
        initSurfaceView()

        // setup the buttons
        findViewById<Button>(R.id.ballpointPen).setOnClickListener(this)
        findViewById<Button>(R.id.fountainPen).setOnClickListener(this)
        findViewById<Button>(R.id.charcoalPen).setOnClickListener(this)
        findViewById<Button>(R.id.charcoalPenV2).setOnClickListener(this)
        findViewById<Button>(R.id.brushPen).setOnClickListener(this)
        findViewById<Button>(R.id.markerPen).setOnClickListener(this)
        findViewById<ImageButton>(R.id.clearPage).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ballpointPen -> binding.writerview.pen = PenType.Ballpoint
            R.id.fountainPen -> binding.writerview.pen = PenType.Fountain
            R.id.charcoalPen -> binding.writerview.pen = PenType.Charcoal
            R.id.charcoalPenV2 -> binding.writerview.pen = PenType.CharcoalV2
            R.id.brushPen -> binding.writerview.pen = PenType.Brush
            R.id.markerPen -> binding.writerview.pen = PenType.Marker
            R.id.clearPage -> confirmClearPage()
        }
    }

    public override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        startTouchHelper()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        binding.writerview.redrawSurface()
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false
        touchHelper.closeRawDrawing()
    }

    public override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun initSurfaceView() {
        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val limit = Rect()
                binding.writerview.getGlobalVisibleRect(limit)
                limitRectList.add(limit)
                startTouchHelper()
                binding.writerview.redrawSurface()
                Log.d(TAG, "surfaceCreated")
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        }
        binding.writerview.holder.addCallback(surfaceCallback)
    }

    private fun startTouchHelper() {
        if (limitRectList.size < 1) {
            return
        }
        Log.d(TAG, "startTouchHelper")
        touchHelper.setStrokeWidth(STROKE_WIDTH)
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER)
        touchHelper.setStrokeColor(Color.BLACK)
        touchHelper.setLimitRect(limitRectList, ArrayList()).openRawDrawing()
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.setRawDrawingEnabled(true)
        touchHelper.enableFingerTouch(true)
        touchHelper.isRawDrawingRenderEnabled = true
    }

    private fun confirmClearPage() {
        // delete a page
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.clear_page_title))
        builder.setMessage(resources.getString(R.string.confirm_clear_page))
        builder.setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
            touchHelper.setRawDrawingEnabled(false)
            touchHelper.isRawDrawingRenderEnabled = false
            touchHelper.closeRawDrawing()
            try {
                binding.writerview.resetBitmap()
                binding.writerview.redrawSurface()
                startTouchHelper()
            } catch (e: Exception) {
                Log.d("loadBitmap Error: ", e.message, e)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
    }

    companion object {
        private val TAG = WriterActivity::class.java.simpleName
        private const val STROKE_WIDTH = 4.0f
    }
}
