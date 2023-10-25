package com.onyx.dailydiary.ical

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onyx.dailydiary.R
import com.onyx.dailydiary.ical.iCalAdapter.CustomViewHolder
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import kotlin.random.Random

class CalendarActivity : AppCompatActivity(), iCalAdapter.OnItemListener, View.OnClickListener {
    private lateinit var mAdapter: iCalAdapter
    private lateinit var calName: EditText
    private lateinit var calUrl: EditText
    private val calendarList = mutableListOf<List<String>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendars)
        supportActionBar!!.hide()
        initData()
        initRecyclerView()
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener(this)
        val addButton = findViewById<ImageButton>(R.id.cal_add_button)
        addButton.setOnClickListener(this)
        calName = findViewById(R.id.name_text_input)
        calUrl = findViewById(R.id.url_text_input)
    }

    private fun initData() {
        Log.d(TAG, "initData")
        val calendarFile = File(getExternalFilesDir(FILE_PATH), FILE_NAME)
        if (calendarFile.exists()) {
            val fis = try {
                FileInputStream(calendarFile)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
            val reader = BufferedReader(InputStreamReader(fis))
            var line: String?
            try {
                line = reader.readLine()
                while (line != null) {
                    val splitLine =
                        line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (splitLine.size == 3) {
                        val calendarLine = ArrayList<String>(3)
                        calendarLine.add(splitLine[0])
                        calendarLine.add(splitLine[1])
                        calendarLine.add(splitLine[2])
                        calendarList.add(calendarLine)
                    }
                    line = reader.readLine()
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun initRecyclerView() {
        val mRecentRecyclerView = findViewById<View>(R.id.calendarICSRecyclerView) as RecyclerView
        mRecentRecyclerView.setHasFixedSize(false)
        mRecentRecyclerView.layoutManager = LinearLayoutManager(this)
        mAdapter = iCalAdapter(this, calendarList)
        mRecentRecyclerView.adapter = mAdapter
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.back_button -> finish()
            R.id.cal_add_button -> addCalToList()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    public override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        val calendarFile = File(getExternalFilesDir(FILE_PATH), FILE_NAME)
        FileWriter(calendarFile).use {
            val writer = FileWriter(calendarFile)
            for (i in calendarList.indices) {
                writer.write(calendarList[i][0] + ",")
                writer.write(calendarList[i][1] + ",")
                writer.write(
                    """
                    ${calendarList[i][2]}
                    
                    """.trimIndent()
                )
            }
        }
        super.onDestroy()
    }

    private fun addCalToList() {
        calendarList.add(
            listOf(
                calName.text.toString().replace(",", ""),
                calUrl.text.toString().replace(",", ""),
                randomFileName(),
            )
        )
        mAdapter.notifyItemInserted(calendarList.size)
        calName.setText("")
        calUrl.setText("")
    }

    override fun onItemClick(position: Int, holder: CustomViewHolder?) {
        calendarList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }

    private fun randomFileName(): String {
        val generatedString = buildString {
            repeat(RANDOM_STRING_LENGTH) {
                appendCodePoint(Random.nextInt(26) + A_CODE_POINT)
            }
        }
        return "$generatedString.ics"
    }

    companion object {
        private val TAG = CalendarActivity::class.java.simpleName
        private const val FILE_PATH = "Calendars"
        private const val FILE_NAME = "calendar_list.txt"
        private const val RANDOM_STRING_LENGTH = 10
        private const val A_CODE_POINT = 97
    }
}
