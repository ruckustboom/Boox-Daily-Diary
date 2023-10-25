package com.onyx.dailydiary.calendar

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onyx.dailydiary.R

class CalendarViewHolder(
    itemView: View,
    private val onItemListener: CalendarAdapter.OnItemListener,
) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    val dayOfMonth: TextView = itemView.findViewById(R.id.cellDayText)
    val currentDay: ImageView = itemView.findViewById(R.id.selectedDay)
    val layout: LinearLayout = itemView.findViewById(R.id.cellDayLayout)
    val headerLayout: LinearLayout = itemView.findViewById(R.id.headerLayout)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        onItemListener.onItemClick(adapterPosition, dayOfMonth.text as String, this)
    }
}