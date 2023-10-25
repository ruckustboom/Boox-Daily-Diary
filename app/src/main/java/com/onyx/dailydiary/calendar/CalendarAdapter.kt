package com.onyx.dailydiary.calendar

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.onyx.dailydiary.MainActivity.HolderListener
import com.onyx.dailydiary.R
import java.time.LocalDate

class CalendarAdapter(
    private val daysOfMonth: ArrayList<String>,
    private val selectedDate: LocalDate,
    private val onItemListener: OnItemListener,
    private val holderListener: HolderListener,
) : RecyclerView.Adapter<CalendarViewHolder>() {
    private var todayHolder: CalendarViewHolder? = null
    private val currentDate: LocalDate = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_cell, parent, false)
        view.layoutParams.height = (parent.height * 0.166666666).toInt()
        return CalendarViewHolder(view, onItemListener)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        if (daysOfMonth[position] == "") {
            holder.layout.setBackgroundColor(Color.LTGRAY)
            holder.headerLayout.setBackgroundColor(Color.LTGRAY)
        }
        holder.dayOfMonth.text = daysOfMonth[position]
        holder.currentDay.setImageResource(R.drawable.white_circle)
        if (selectedDate.monthValue == currentDate.monthValue && selectedDate.year == currentDate.year) {
            try {
                if (currentDate.dayOfMonth == daysOfMonth[position].toInt()) {
                    holder.currentDay.setImageResource(R.drawable.filled_circle)
                    todayHolder = holder
                    holderListener.onTodayHolderCreated(todayHolder)
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

    interface OnItemListener {
        fun onItemClick(position: Int, dayText: String?, holder: CalendarViewHolder?)
    }

    companion object {
        private val TAG = CalendarAdapter::class.java.simpleName
    }
}
