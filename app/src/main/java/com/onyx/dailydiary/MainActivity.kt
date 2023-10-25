package com.onyx.dailydiary

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.dailydiary.calendar.CalendarAdapter
import com.onyx.dailydiary.calendar.CalendarViewHolder
import com.onyx.dailydiary.databinding.ActivityMainBinding
import com.onyx.dailydiary.ical.CalendarActivity
import com.onyx.dailydiary.ical.iCalParser
import com.onyx.dailydiary.utils.BitmapView
import com.onyx.dailydiary.utils.GlobalDeviceReceiver
import com.onyx.dailydiary.utils.PenCallback
import com.onyx.dailydiary.writer.WriterActivity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), CalendarAdapter.OnItemListener, View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var monthText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var selectedDate: LocalDate
    private lateinit var touchHelper: TouchHelper
    private var lastHolder: CalendarViewHolder? = null
    private var dayOfMonth: String? = null
    private val limitRectList = mutableListOf<Rect>()
    private lateinit var parser: iCalParser
    private val deviceReceiver = GlobalDeviceReceiver()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        deviceReceiver.enable(this, true)
        val view: View = binding.root
        setContentView(view)
        selectedDate = LocalDate.now()
        initWidgets()
        initReceiver()
        val formatter = DateTimeFormatter.ofPattern("d")
        dayOfMonth = selectedDate.format(formatter)
        val viewList: MutableList<BitmapView> = ArrayList()
        viewList.add(binding.taskssurfaceview)
        viewList.add(binding.summarysurfaceview)
        val penCallback = PenCallback(this, viewList)
        touchHelper = TouchHelper.create(window.decorView.rootView, penCallback)
        touchHelper.debugLog(false)
        touchHelper.setRawInputReaderEnable(true)
        penCallback.setTouchHelper(touchHelper)
        val summaryFilename = "$currentDateString.png"
        val tasksFilename = "tasks.png"
        initSurfaceView(binding.taskssurfaceview, tasksFilename, R.drawable.tasks_bkgrnd)
        initSurfaceView(binding.summarysurfaceview, summaryFilename, R.drawable.summary_bkgrnd)
        view.findViewById<Button>(R.id.clearsummary).setOnClickListener(this)
        view.findViewById<Button>(R.id.opendiary).setOnClickListener(this)
        view.findViewById<Button>(R.id.clear_tasks).setOnClickListener(this)
        parser = iCalParser(applicationContext)
        parser.loadCalendars()
        setMonthView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    public override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false
        touchHelper.closeRawDrawing()
        binding.taskssurfaceview.redrawSurface()
        binding.summarysurfaceview.redrawSurface()
        binding.taskssurfaceview.saveBitmap()
        binding.summarysurfaceview.saveBitmap()
    }

    public override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        startTouchHelper()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clear_tasks -> {
                binding.taskssurfaceview.resetBitmap()
                binding.taskssurfaceview.redrawSurface()
            }
            R.id.clearsummary -> {
                binding.summarysurfaceview.resetBitmap()
                binding.summarysurfaceview.redrawSurface()
            }
            R.id.opendiary -> {
                binding.taskssurfaceview.saveBitmap()
                binding.summarysurfaceview.saveBitmap()
                openPage()
            }
        }
        Log.d(TAG, "onClick")
    }

    override fun onItemClick(position: Int, dayText: String?, holder: CalendarViewHolder?) {
        if (dayText != "") {
            binding.summarysurfaceview.saveBitmap()
            dayOfMonth = dayText
            val summaryFilename = "$currentDateString.png"
            binding.summarysurfaceview.fileName = summaryFilename
            binding.summarysurfaceview.redrawSurface()
            if (lastHolder != null) {
                lastHolder!!.layout.setBackgroundColor(Color.WHITE)
                lastHolder!!.headerLayout.setBackgroundColor(Color.WHITE)
                lastHolder!!.eventsText.setTextColor(Color.BLACK)
                lastHolder!!.dayOfMonth.setTextColor(Color.BLACK)
            }
            holder!!.layout.setBackgroundColor(Color.DKGRAY)
            holder.dayOfMonth.setTextColor(Color.WHITE)
            holder.eventsText.setTextColor(Color.WHITE)
            holder.headerLayout.setBackgroundColor(Color.DKGRAY)
            lastHolder = holder
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_reload) {
            parser.syncCalendars()
            setMonthView()
        } else if (itemId == R.id.edit_calendars) { // opening a new intent to open calendar settings activity.
            val i = Intent(this@MainActivity, CalendarActivity::class.java)
            startActivity(i)
        }
        return true
    }

    private fun initReceiver() {
        deviceReceiver.callbacks = object : GlobalDeviceReceiver.Callbacks {
            override fun onNotificationPanel(open: Boolean) {
                touchHelper.setRawDrawingEnabled(!open)
                Log.d(TAG, "onNotificationPanelChanged $open")
                binding.taskssurfaceview.saveBitmap()
                binding.summarysurfaceview.saveBitmap()
            }

            override fun onScreenOn() {
                Log.d(TAG, "onScreenOn")
                selectedDate = LocalDate.now()
                setMonthView()
            }

            override fun onScreenOff() {
                Log.d(TAG, "onScreenOff")
                onPause()
            }
        }
    }

    private fun initSurfaceView(surfaceView: BitmapView, filename: String, background: Int) {
        surfaceView.background = background
        val filepath = "DailyNotes"
        surfaceView.setFilepath(filepath)
        surfaceView.fileName = filename
        Log.d(TAG, "initSurfaceView")
        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Tasks surfaceCreated")
                val limit = Rect()
                surfaceView.getGlobalVisibleRect(limit)
                limitRectList.add(limit)
                startTouchHelper()
                surfaceView.redrawSurface()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "Tasks surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                holder.removeCallback(this)
            }
        }
        surfaceView.holder.addCallback(surfaceCallback)
    }

    private fun initWidgets() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        monthText = findViewById(R.id.monthTV)
    }

    private fun startTouchHelper() {
        if (limitRectList.size < 2) return
        touchHelper.setStrokeWidth(STROKE_WIDTH)
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER)
        touchHelper.setStrokeColor(Color.BLACK)
        touchHelper.setLimitRect(limitRectList, ArrayList()).openRawDrawing()
        touchHelper.setRawDrawingEnabled(false)
        touchHelper.setSingleRegionMode()
        touchHelper.setRawDrawingEnabled(true)
        touchHelper.enableFingerTouch(true)
        touchHelper.isRawDrawingRenderEnabled = true
    }

    private fun openPage() {
        try {
            val intent = Intent(this@MainActivity, WriterActivity::class.java)
            intent.putExtra("date-string", currentDateString) //Optional parameters
            intent.putExtra("stroke-width", STROKE_WIDTH)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Unable to open daily notes.", Toast.LENGTH_LONG)
                .show()
        }
    }

    private val currentDateString: String
        get() = dayOfMonth + "-" + monthYearFromDate(selectedDate)

    private fun setMonthView() {
        Log.d(TAG, "setMonthView")
        monthText.text = monthFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)
        val holderListener = HolderListener(::onTodayHolderCreated)
        val calendarAdapter =
            CalendarAdapter(parser, daysInMonth, selectedDate, this, holderListener)
        val layoutManager = GridLayoutManager(applicationContext, 7)
        calendarRecyclerView.adapter = null
        calendarRecyclerView.layoutManager = null
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
        calendarAdapter.notifyDataSetChanged()
    }

    fun interface HolderListener {
        fun onTodayHolderCreated(holder: CalendarViewHolder?)
    }

    private fun onTodayHolderCreated(holder: CalendarViewHolder?) {
        lastHolder = holder
    }

    private fun daysInMonthArray(date: LocalDate?): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value
        for (i in 1..42) {
            if (i < dayOfWeek || i >= daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek + 1).toString())
            }
        }
        return daysInMonthArray
    }

    private fun monthYearFromDate(date: LocalDate?): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM-yyyy")
        return date!!.format(formatter)
    }

    private fun monthFromDate(date: LocalDate?): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM")
        return date!!.format(formatter)
    }

    fun previousMonthAction(view: View?) {
        selectedDate = selectedDate.minusMonths(1)
        setMonthView()
    }

    fun nextMonthAction(view: View?) {
        selectedDate = selectedDate.plusMonths(1)
        setMonthView()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val STROKE_WIDTH = 4.0f
    }
}
