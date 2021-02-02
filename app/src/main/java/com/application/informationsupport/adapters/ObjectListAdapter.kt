package com.application.informationsupport.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.R
import com.application.informationsupport.models.ModelObjectList

class ObjectListAdapter (val context: Context, val objectList: MutableList<ModelObjectList>) :
    RecyclerView.Adapter<ObjectListAdapter.MyHolder>() {

    class MyHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        var typeIV: ImageView = itemView.findViewById(R.id.imageView)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_data_object, parent, false)
        return MyHolder(view)
    }

    override fun getItemCount(): Int {
        return objectList.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val name = objectList[position].name
        val creator = objectList[position].creator
        val date = objectList[position].date
        val isFolder = objectList[position].isFolder

        holder.nameTV.text = name
        holder.creatorTV.text = creator
        holder.dateTV.text = date
        if (isFolder) holder.typeIV.setImageResource(R.drawable.ic_folder)

        holder.itemView.setOnClickListener {
            TODO()
        }
    }


}