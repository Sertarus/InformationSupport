package com.application.informationsupport.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.R
import com.application.informationsupport.models.ModelDataItem

class DataItemAdapter(val context: Context, private val itemList: List<ModelDataItem>) :
    RecyclerView.Adapter<DataItemAdapter.DataItemHolder>() {

    class DataItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fieldTV: TextView = itemView.findViewById(R.id.fieldNameTV)
        var dataTV: TextView = itemView.findViewById(R.id.dataTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataItemHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_data_item, parent, false)
        return DataItemHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: DataItemHolder, position: Int) {
        val field = itemList[position].fieldName
        val data = itemList[position].data
        val fullField = "$field: "
        holder.fieldTV.text = fullField
        holder.dataTV.text = data
    }


}