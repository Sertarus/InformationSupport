package com.application.informationsupport.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.R

class CheckAdapter (val context: Activity, var checkList: MutableList<Pair<String, Boolean>>, val type: String) : RecyclerView.Adapter<CheckAdapter.CheckHolder>() {

    class CheckHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
        var checkbox: AppCompatCheckBox = itemView.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_check, parent, false)
        return CheckHolder(view)
    }

    override fun getItemCount(): Int {
        return checkList.size
    }

    override fun onBindViewHolder(holder: CheckHolder, position: Int) {
        holder.nameTV.text = checkList[position].first
        if (type == "service") holder.imageView.setImageResource(R.drawable.ic_service)
        else holder.imageView.setImageResource(R.drawable.ic_district)
        holder.itemView.setOnClickListener {
            checkList[position] = Pair(checkList[position].first, !checkList[position].second)
            holder.checkbox.isChecked = checkList[position].second
            this.notifyDataSetChanged()
        }
    }
}