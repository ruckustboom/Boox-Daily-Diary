package com.onyx.dailydiary.ical

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.onyx.dailydiary.R
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.filter.Filter
import net.fortuna.ical4j.filter.PeriodRule
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Dur
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.MapTimeZoneCache
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.util.stream.Collectors

class iCalParser(private val context: Context) {
    private var calendarList = mutableListOf<List<String>>()
    private var iCalCalendars = mutableListOf<Calendar>()
    private var iCalTimeZones = mutableListOf<TimeZoneRegistry>()

    init {
        System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            MapTimeZoneCache::class.java.name,
        )
        Log.d(TAG, "iCalParser")
    }

    private fun loadCalendarList() {
        calendarList.clear()
        val calendarFile = File(context.getExternalFilesDir(FILE_PATH), FILE_NAME)
        if (calendarFile.exists()) {
            val fis = try {
                FileInputStream(calendarFile)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
            val reader = BufferedReader(InputStreamReader(fis))
            try {
                while (true) {
                    val splitLine = (reader.readLine() ?: break)
                        .split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                    Log.d(TAG, splitLine.toString())
                    if (splitLine.size == 3) {
                        calendarList.add(splitLine.take(3))
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun loadCalendars() {
        iCalCalendars.clear()
        iCalTimeZones.clear()
        loadCalendarList()
        for (x in calendarList) {
            val icsName = x[0]
            val icsFileName = x[2]
            Log.d(TAG, "Loading $icsName")
            try {
                val file = File(context.getExternalFilesDir(FILE_PATH), icsFileName)
                val fin = FileInputStream(file.path)
                val builder = CalendarBuilder()
                val calendar = builder.build(fin)
                iCalCalendars.add(calendar)
                val registry = builder.registry
                iCalTimeZones.add(registry)
            } catch (e: Exception) {
                Log.d(TAG, e.message!!)
            }
        }
    }

    fun getDayEvents(inputDate: LocalDate): List<String?> {
        Log.d(TAG, "get_day_events")
        var eventList: MutableList<String?> = ArrayList()
        for (i in iCalCalendars.indices) {
            val calendar = iCalCalendars[i]
            val registry = iCalTimeZones[i]
            try {
                val selectedDate = DateTime(
                    Date.from(
                        inputDate.atStartOfDay()
                            .plusSeconds(1)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                    )
                )
                val period = Period(selectedDate, Dur(0, 23, 59, 0))
                val periodRule = PeriodRule<CalendarComponent>(period)
                val filter = Filter(arrayOf(periodRule), Filter.MATCH_ANY)
                val events = filter.filter(calendar.getComponents(CalendarComponent.VEVENT))
                val fullFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                for (c in events) {
                    try {
                        val event = c as VEvent
                        var summary = event.getProperty<Property>("SUMMARY").value
                        val eventTimeStr = event.getProperty<Property>("DtStart").value
                        val eventTZ =
                            event.getProperty<Property>("DtStart").getParameter<Parameter>("TZID")
                        if (eventTZ != null) {
                            val calendarTZ = registry.getTimeZone(
                                eventTZ.toString().split("=".toRegex())
                                    .dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[1])
                            val calendarToUTC = calendarTZ.getOffset(Instant.now().toEpochMilli())
                            val systemTZ = TimeZone.getDefault()
                            val utcToSystem = systemTZ.getOffset(Instant.now().toEpochMilli())
                            var eventDateTime =
                                LocalDateTime.parse(eventTimeStr.substring(0, 15), fullFormatter)
                            eventDateTime =
                                eventDateTime.minusSeconds((calendarToUTC / 1000.0).toLong())
                            eventDateTime =
                                eventDateTime.plusSeconds((utcToSystem / 1000.0).toLong())
                            if (eventDateTime.hour + eventDateTime.minute != 0) {
                                val formattedTime = eventDateTime.format(timeFormatter)
                                summary = "$formattedTime $summary"
                            }
                        }
                        eventList.add(summary)
                    } catch (e: Exception) {
                        Log.d(TAG, e.message!!)
                    }
                }

                // remove duplicates
                eventList = eventList.stream().distinct().collect(Collectors.toList())
            } catch (e: Exception) {
                Log.d(TAG, e.message!!)
            }
        }
        return eventList
    }

    fun syncCalendars() {
        loadCalendarList()
        Toast.makeText(
            context,
            context.resources.getString(R.string.downloading_calendars),
            Toast.LENGTH_LONG
        ).show()
        val thread = Runnable {
            for (i in calendarList.indices) {
                val icsfilename = calendarList[i][2]
                val icsurl = calendarList[i][1]
                val icsname = calendarList[i][0]
                try {
                    val input = URL(icsurl).openStream()
                    val builder = CalendarBuilder()
                    val calendar = builder.build(input)
                    val calendarFile = File(context.getExternalFilesDir(FILE_PATH), icsfilename)
                    val fout = FileOutputStream(calendarFile)
                    val outputter = CalendarOutputter()
                    outputter.isValidating = false
                    outputter.output(calendar, fout)
                    fout.close()
                } catch (e: Exception) {
                    Log.d(TAG, e.message!!)
                }
            }
            loadCalendars()
        }
        Thread(thread).start()
    }

    companion object {
        private val TAG = iCalParser::class.java.simpleName
        private const val FILE_PATH = "Calendars"
        private const val FILE_NAME = "calendar_list.txt"
    }
}
