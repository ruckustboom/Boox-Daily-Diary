package com.onyx.dailydiary.ical

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onyx.dailydiary.R
import com.onyx.dailydiary.ical.iCalAdapter.CustomViewHolder

class iCalAdapter(
    private val onItemListener: OnItemListener,
    private val calendarList: List<List<String>>,
) : RecyclerView.Adapter<CustomViewHolder?>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): CustomViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.ics_text_row_item, viewGroup, false)
        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder")
        holder.textViewCalName.text = calendarList[position][0]
        holder.textViewICS.text = calendarList[position][1]
    }

    override fun getItemCount(): Int {
        return calendarList.size
    }

    interface OnItemListener {
        fun onItemClick(position: Int, holder: CustomViewHolder?)
    }

    inner class CustomViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        internal val textViewCalName: TextView
        internal val textViewICS: TextView

        init {
            textViewCalName = itemView.findViewById<View>(R.id.textViewCalName) as TextView
            textViewICS = itemView.findViewById<View>(R.id.textViewICS) as TextView
            val add_button = itemView.findViewById<ImageButton>(R.id.deletecalendar)
            add_button.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            onItemListener.onItemClick(adapterPosition, this)
        }
    }

    companion object {
        private val TAG = iCalAdapter::class.java.simpleName
    }
}
