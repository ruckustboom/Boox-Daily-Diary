package com.onyx.dailydiary.writer

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.dailydiary.R
import com.onyx.dailydiary.databinding.ActivityWriterBinding
import com.onyx.dailydiary.utils.BitmapView
import com.onyx.dailydiary.utils.GestureListener
import com.onyx.dailydiary.utils.PenCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class WriterActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityWriterBinding
    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var touchHelper: TouchHelper
    private val filepath = "DailyDiary"
    private var filename: String? = null
    private val limitRectList: MutableList<Rect> = ArrayList()
    private lateinit var penCallback: PenCallback
    private var dayPage = 0
    private var dayPageCount = 0
    private lateinit var datebox: TextView
    private lateinit var currentDate: LocalDate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        binding = ActivityWriterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val viewList: MutableList<BitmapView> = ArrayList()
        viewList.add(binding.writerview)
        penCallback = PenCallback(this, viewList)
        touchHelper = TouchHelper.create(window.decorView.rootView, penCallback).apply {
            debugLog(false)
            setRawInputReaderEnable(true)
        }
        penCallback.setTouchHelper(touchHelper)

        // setup the gestures
        mDetector = GestureDetectorCompat(this, object : GestureListener() {
            override fun onSwipeBottom() {
                if (!penCallback.isRawDrawing) {
                    deletePage()
                }
            }

            override fun onSwipeLeft() {
                if (!penCallback.isRawDrawing) {
                    updatePage(true)
                }
            }

            override fun onSwipeRight() {
                if (!penCallback.isRawDrawing) {
                    updatePage(false)
                }
            }

            override fun onSwipeTop() {
                if (!penCallback.isRawDrawing) {
                    addPage()
                }
            }
        })


        // setup the date
        currentDate = LocalDate.parse(
            intent.getStringExtra("date-string"),
            DateTimeFormatter.ofPattern("d-MMMM-yyyy")
        )
        dayPage = 1
        dayPageCount = countDayPages()
        datebox = findViewById(R.id.date_text)
        datebox.text =
            currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + dayPage + "/" + dayPageCount + ")"
        filename = currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + dayPage + ".png"
        // initialise surface
        initSurfaceView()

        // setup the buttons
        findViewById<ImageButton>(R.id.back_button).setOnClickListener(this)
        findViewById<ImageButton>(R.id.nextpage).setOnClickListener(this)
        findViewById<ImageButton>(R.id.prevpage).setOnClickListener(this)
        findViewById<ImageButton>(R.id.addpage).setOnClickListener(this)
        findViewById<ImageButton>(R.id.deletepage).setOnClickListener(this)
        findViewById<ImageButton>(R.id.save).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.back_button -> onBackPressed()
            R.id.nextpage -> updatePage(true)
            R.id.prevpage -> updatePage(false)
            R.id.addpage -> addPage()
            R.id.deletepage -> deletePage()
            R.id.save -> savePages()
        }
    }

    public override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        startTouchHelper()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        binding.writerview.redrawSurface()
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false
        touchHelper.closeRawDrawing()
        if (penCallback.isDirty) {
            binding.writerview.saveBitmap()
        }
    }

    public override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        if (penCallback.isDirty) {
            binding.writerview.saveBitmap()
        }
    }

    private fun initSurfaceView() {
        binding.writerview.background = R.drawable.page_bkgrnd
        binding.writerview.setFilepath(filepath)
        binding.writerview.fileName = filename
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
                height: Int
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
        touchHelper.setMultiRegionMode()
        touchHelper.setRawDrawingEnabled(true)
        touchHelper.enableFingerTouch(true)
        touchHelper.isRawDrawingRenderEnabled = true
    }

    private fun countDayPages(): Int {
        val dir = getExternalFilesDir(filepath)
        val files = dir!!.listFiles { _, name ->
            name.contains(currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
        }
        return Integer.max(files.size, 1)
    }

    private fun updatePage(forward: Boolean) {
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false
        touchHelper.closeRawDrawing()

        // move forward or backwards in the diary
        if (penCallback.isDirty) {
            binding.writerview.saveBitmap()
        }
        if (forward) {
            if (dayPage < dayPageCount) {
                dayPage++
            } else {
                dayPage = 1
                currentDate = currentDate.plusDays(1)
                dayPageCount = countDayPages()
            }
        } else {
            if (dayPage > 1) {
                dayPage--
            } else {
                currentDate = currentDate.plusDays(-1)
                dayPageCount = countDayPages()
                dayPage = dayPageCount
            }
        }
        datebox = findViewById(R.id.date_text)
        datebox.text =
            currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + dayPage + "/" + dayPageCount + ")"
        filename = currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + dayPage + ".png"
        binding.writerview.fileName = filename
        binding.writerview.redrawSurface()
        startTouchHelper()
    }

    private fun addPage() {
        // add a page to the end and move forward
        penCallback.isDirty = true
        binding.writerview.saveBitmap()
        dayPageCount++
        updatePage(true)
    }

    private fun deletePage() {
        // delete a page
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.delete_title))
        builder.setMessage(resources.getString(R.string.confirm_delete))
        builder.setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
            val deletePage = dayPage
            touchHelper.setRawDrawingEnabled(false)
            touchHelper.isRawDrawingRenderEnabled = false
            touchHelper.closeRawDrawing()
            try {
                var externalFile: File
                externalFile = File(getExternalFilesDir(filepath), filename)
                if (externalFile.exists()) {
                    externalFile.delete()
                }
                for (i in dayPage until dayPageCount) {
                    println(i)
                    val newfilename =
                        currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + i.toString() + ".png"
                    val oldfilename =
                        currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + (i + 1).toString() + ".png"
                    externalFile = File(getExternalFilesDir(filepath), oldfilename)
                    val newExternalFile = File(getExternalFilesDir(filepath), newfilename)
                    if (externalFile.exists()) {
                        externalFile.renameTo(newExternalFile)
                    }
                }
                penCallback.isDirty = false
                if (dayPageCount != 1) dayPageCount--
                if (deletePage != 1) dayPage--
                datebox = findViewById(R.id.date_text)
                datebox.text =
                    currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + dayPage + "/" + dayPageCount + ")"
                filename =
                    currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + dayPage + ".png"
                binding.writerview.fileName = filename
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

    private fun savePages() {
        // let the user choose a time frame for export then export to pdf
        binding.writerview.redrawSurface()
        binding.writerview.saveBitmap()
        val builder = AlertDialog.Builder(this, R.style.MaterialThemeDialog)
        val item = arrayOf(
            resources.getString(R.string.day),
            resources.getString(R.string.month),
            resources.getString(R.string.year)
        )
        val timeframe = intArrayOf(0)
        builder.setTitle(resources.getString(R.string.export_title))
            .setSingleChoiceItems(item, 0) { _, i -> timeframe[0] = i }
            // Set the action buttons
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                try {
                    writeToPDF(timeframe[0])
                } catch (e: FileNotFoundException) {
                    throw RuntimeException(e)
                }
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
        val alert = builder.create()
        alert.setOnShowListener { _ ->
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
        }
        alert.show()
    }

    @Throws(FileNotFoundException::class)
    private fun writeToPDF(timeframe: Int) {
        // this code makes a pdf from the pages of the diary and opens it
        Toast.makeText(this, resources.getString(R.string.export_in_progress), Toast.LENGTH_LONG)
            .show()
        val startDate: LocalDate?
        val endDate: LocalDate?
        val outputFilename: String
        when (timeframe) {
            0 -> {
                startDate = currentDate
                endDate = currentDate
                outputFilename =
                    "Diary-" + currentDate.format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")) + ".pdf"
            }

            1 -> {
                startDate = currentDate.with(TemporalAdjusters.firstDayOfMonth())
                endDate = currentDate.with(TemporalAdjusters.lastDayOfMonth())
                outputFilename =
                    "Diary-" + currentDate.format(DateTimeFormatter.ofPattern("MMMM-yyyy")) + ".pdf"
            }

            2 -> {
                startDate = currentDate.with(TemporalAdjusters.firstDayOfYear())
                endDate = currentDate.with(TemporalAdjusters.lastDayOfYear())
                outputFilename =
                    "Diary-" + currentDate.format(DateTimeFormatter.ofPattern("yyyy")) + ".pdf"
            }

            else -> {
                startDate = currentDate
                endDate = currentDate
                outputFilename = ""
            }
        }
        val pdfDocument = PdfDocument()
        val pageHeight = 2200
        val pageWidth = 1650
        var myPageInfo: PdfDocument.PageInfo?
        var startPage: PdfDocument.Page
        var printDate = startDate
        while (!printDate!!.isAfter(endDate)) {
            val dir = getExternalFilesDir(filepath)
            val finalPrintDate = printDate
            val files = dir!!.listFiles { _, name ->
                name.contains(finalPrintDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
            }
            val length = files.size
            for (printPage in 1..length) {
                val pageTitle =
                    printDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + printPage + "/" + length + ")"
                val printFileName =
                    printDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + printPage + ".png"
                val bitmapFile = File(getExternalFilesDir(filepath), printFileName)
                if (bitmapFile.exists()) {
                    val opt = BitmapFactory.Options()
                    opt.inScaled = true
                    opt.inMutable = true
                    val bitmap = BitmapFactory.decodeStream(FileInputStream(bitmapFile), null, opt)
                    myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    startPage = pdfDocument.startPage(myPageInfo)
                    val title = Paint()
                    title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    title.textSize = 40f
                    title.color = Color.WHITE
                    val myPaint = Paint()
                    myPaint.color = Color.rgb(0, 0, 0)
                    myPaint.strokeWidth = 10f
                    startPage.canvas.apply {
                        drawRect(0f, 0f, pageWidth.toFloat(), 122f, myPaint)
                        drawText(pageTitle, 110f, 100f, title)
                        drawBitmap(bitmap!!, 0f, 122f, null)
                    }
                    pdfDocument.finishPage(startPage)
                }
            }
            printDate = printDate.plusDays(1)
        }
        val resolver = this.contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outputFilename)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        val path = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        try {
            pdfDocument.writeTo(resolver.openOutputStream(path!!))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        pdfDocument.close()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(path, "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        startActivity(intent)
    }

    companion object {
        private val TAG = WriterActivity::class.java.simpleName
        private const val STROKE_WIDTH = 4.0f
    }
}