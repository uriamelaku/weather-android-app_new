package com.example.weather_check

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_check.models.WeatherResponse

class WeatherListAdapter(
    private val onDeleteClick: (WeatherResponse) -> Unit
) : RecyclerView.Adapter<WeatherListAdapter.WeatherItemViewHolder>() {

    private val items = mutableListOf<WeatherResponse>()

    fun submitItems(newItems: List<WeatherResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_list, parent, false)
        return WeatherItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherItemViewHolder, position: Int) {
        holder.bind(items[position], onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class WeatherItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.tvItemSubtitle)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteItem)

        fun bind(item: WeatherResponse, onDeleteClick: (WeatherResponse) -> Unit) {
            title.text = "${item.city}, ${item.country}"
            subtitle.text = "${item.temp.toInt()}° - ${item.description}"
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }
}

